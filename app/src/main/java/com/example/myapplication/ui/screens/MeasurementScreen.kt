package com.example.myapplication.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.measurement.MeasurementPoint
import com.example.myapplication.ui.viewmodel.MeasurementMode
import com.example.myapplication.ui.viewmodel.MeasurementViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(viewModel: MeasurementViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    LaunchedEffect(uiState.currentCameraIndex) {
        previewView?.let { preview ->
            setupCamera(viewModel, preview, lifecycleOwner)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Vista de la cámara
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        viewModel.addMeasurementPoint(
                            offset.x, offset.y,
                            size.width, size.height
                        )
                    }
                },
            factory = { ctx ->
                PreviewView(ctx).also { 
                    previewView = it
                    setupCamera(viewModel, it, lifecycleOwner)
                }
            }
        )
        
        // Overlay con puntos de medición
        MeasurementOverlay(
            measurementPoints = viewModel.getMeasurementEngine().getCurrentMeasurementPoints(),
            modifier = Modifier.fillMaxSize()
        )
        
        // Panel de controles superior
        TopControlPanel(
            uiState = uiState,
            onModeChange = { viewModel.setMeasurementMode(it) },
            onCameraSwitch = { viewModel.switchCamera(it) },
            onSettingsClick = { viewModel.toggleSettings() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Panel de información lateral
        InfoPanel(
            uiState = uiState,
            stability = viewModel.getSensorStability(),
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        
        // Panel de controles inferior
        BottomControlPanel(
            uiState = uiState,
            onClear = { viewModel.clearMeasurementPoints() },
            onUndo = { viewModel.undoLastPoint() },
            onExport = { viewModel.exportMeasurements() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Panel de configuración
        if (uiState.showSettings) {
            SettingsPanel(
                onDismiss = { viewModel.toggleSettings() },
                onCalibrate = { distance, pixels -> 
                    viewModel.manualCalibration(distance, pixels)
                }
            )
        }
        
        // Mostrar errores
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Mostrar snackbar o toast
                viewModel.clearError()
            }
        }
    }
}

private fun setupCamera(
    viewModel: MeasurementViewModel,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner
) {
    val cameraManager = viewModel.getCameraManager()
    val uiState = viewModel.uiState.value
    
    val cameraSelector = if (uiState.currentCameraIndex < cameraManager.availableCameras.size) {
        val camera = cameraManager.availableCameras[uiState.currentCameraIndex]
        when (camera.facing) {
            android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT -> 
                CameraSelector.DEFAULT_FRONT_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }
    
    val imageAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
        viewModel.processImage(imageProxy)
    }
    
    cameraManager.bindCamera(
        lifecycleOwner = lifecycleOwner,
        cameraSelector = cameraSelector,
        surfaceProvider = previewView.surfaceProvider,
        analyzer = imageAnalyzer
    )
}

@Composable
fun MeasurementOverlay(
    measurementPoints: List<MeasurementPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawMeasurementPoints(measurementPoints)
        drawMeasurementLines(measurementPoints)
    }
}

private fun DrawScope.drawMeasurementPoints(points: List<MeasurementPoint>) {
    points.forEachIndexed { index, point ->
        drawCircle(
            color = when (index) {
                0 -> Color.Green
                1 -> Color.Red
                else -> Color.Blue
            },
            radius = 12.dp.toPx(),
            center = Offset(point.x, point.y)
        )
        
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(point.x, point.y)
        )
    }
}

private fun DrawScope.drawMeasurementLines(points: List<MeasurementPoint>) {
    if (points.size >= 2) {
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color.Yellow,
                start = Offset(points[i].x, points[i].y),
                end = Offset(points[i + 1].x, points[i + 1].y),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

@Composable
fun TopControlPanel(
    uiState: com.example.myapplication.ui.viewmodel.UiState,
    onModeChange: (MeasurementMode) -> Unit,
    onCameraSwitch: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selector de modo
            Row {
                MeasurementMode.values().forEach { mode ->
                    FilterChip(
                        onClick = { onModeChange(mode) },
                        label = { Text(mode.name, fontSize = 10.sp) },
                        selected = uiState.currentMode == mode,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
            
            // Controles de cámara
            Row {
                IconButton(
                    onClick = { 
                        val nextIndex = (uiState.currentCameraIndex + 1) % uiState.availableCameras.size
                        onCameraSwitch(nextIndex)
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Cambiar cámara")
                }
                
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Configuración")
                }
            }
        }
    }
}

@Composable
fun InfoPanel(
    uiState: com.example.myapplication.ui.viewmodel.UiState,
    stability: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(120.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Estado de calibración
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (uiState.isCalibrated) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (uiState.isCalibrated) Color.Green else Color(0xFFFFA500),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (uiState.isCalibrated) "CAL" else "---",
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Estabilidad del dispositivo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Estabilidad", fontSize = 8.sp)
                LinearProgressIndicator(
                    progress = stability,
                    modifier = Modifier
                        .height(4.dp)
                        .width(60.dp),
                    color = when {
                        stability > 0.8f -> Color.Green
                        stability > 0.5f -> Color.Yellow
                        else -> Color.Red
                    }
                )
                Text("${(stability * 100).toInt()}%", fontSize = 8.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Datos del sensor
            Column {
                Text("Inclinación", fontSize = 8.sp)
                Text("${uiState.sensorData.deviceTilt.toInt()}°", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text("Brújula", fontSize = 8.sp)
                Text("${uiState.sensorData.compassHeading.toInt()}°", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BottomControlPanel(
    uiState: com.example.myapplication.ui.viewmodel.UiState,
    onClear: () -> Unit,
    onUndo: () -> Unit,
    onExport: () -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Controles de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onUndo,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deshacer")
                }
                
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar")
                }
                
                Button(
                    onClick = { onExport() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exportar")
                }
            }
            
            // Resultados de medición
            if (uiState.measurementResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 120.dp)
                ) {
                    items(uiState.measurementResults.takeLast(3)) { result ->
                        MeasurementResultItem(result = result)
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementResultItem(
    result: com.example.myapplication.measurement.MeasurementResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = result.type.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.2f", result.value)} ${result.unit}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Indicador de confianza
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            result.confidence > 0.8f -> Color.Green
                            result.confidence > 0.6f -> Color.Yellow
                            else -> Color.Red
                        }.copy(alpha = 0.3f)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(result.confidence * 100).toInt()}%",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SettingsPanel(
    onDismiss: () -> Unit,
    onCalibrate: (Float, Float) -> Unit
) {
    var knownDistance by remember { mutableStateOf("") }
    var pixelDistance by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración y Calibración") },
        text = {
            Column {
                Text("Calibración Manual")
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = knownDistance,
                    onValueChange = { knownDistance = it },
                    label = { Text("Distancia conocida (mm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = pixelDistance,
                    onValueChange = { pixelDistance = it },
                    label = { Text("Distancia en píxeles") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val distance = knownDistance.toFloatOrNull()
                    val pixels = pixelDistance.toFloatOrNull()
                    if (distance != null && pixels != null) {
                        onCalibrate(distance, pixels)
                        onDismiss()
                    }
                }
            ) {
                Text("Calibrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
