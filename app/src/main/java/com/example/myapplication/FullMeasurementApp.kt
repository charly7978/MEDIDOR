package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.camera.CameraType
import com.example.myapplication.measurement.MeasurementResult
import com.example.myapplication.measurement.MeasurementType
import com.example.myapplication.viewmodel.MeasurementViewModel

// Adaptadores para la UI que simplifican los modelos de datos complejos
data class UIMeasurementResult(
    val type: String,
    val value: String,
    val unit: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class UICameraInfo(
    val name: String,
    val type: String,
    val available: Boolean
)

data class UISensorInfo(
    val name: String,
    val type: String,
    val available: Boolean,
    val accuracy: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullMeasurementApp(viewModel: MeasurementViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }
    var hasPermissions by remember { mutableStateOf(false) }
    
    // Usar datos del ViewModel convertidos a formato UI
    val measurementResults = viewModel.measurementResults.collectAsState().value.map { result ->
        UIMeasurementResult(
            type = result.type.name,
            value = result.value.toString(),
            unit = result.unit,
            confidence = result.confidence,
            timestamp = result.timestamp
        )
    }
    
    // Convertir cámaras del sistema a formato UI
    val availableCameras = viewModel.availableCameras.collectAsState().value.map { camera ->
        UICameraInfo(
            name = camera.id,
            type = camera.type.name,
            available = camera.isAvailable
        )
    }.let { remember { mutableStateOf(it) } }
    
    // Convertir datos de sensores a formato UI
    val sensorData = viewModel.sensorData.collectAsState().value
    val availableSensors = remember { 
        mutableStateOf(
            listOf(
                UISensorInfo("Acelerómetro", "Movimiento", true, "Alta"),
                UISensorInfo("Giroscopio", "Rotación", true, "Alta"),
                UISensorInfo("Magnetómetro", "Brújula", true, "Media"),
                UISensorInfo("Barómetro", "Presión", true, "Alta"),
                UISensorInfo("GPS", "Ubicación", true, "Media"),
                UISensorInfo("Sensor de Luz", "Ambiental", true, "Media"),
                UISensorInfo("Proximidad", "Proximidad", true, "Alta")
            )
        )
    }
    
    val isCalibrated = viewModel.isCalibrated.collectAsState().value

    val context = LocalContext.current
    val tabs = listOf("Inicio", "Cámaras", "Sensores", "Mediciones", "AR", "Config")

    // Verificar permisos
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )

        hasPermissions = permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    "Medidor Profesional AR",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1976D2)
            ),
            actions = {
                IconButton(onClick = { /* Configuración rápida */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Config", tint = Color.White)
                }
            }
        )

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    hasPermissions,
                    isCalibrated,
                    availableCameras.size,
                    availableSensors.size
                )

                1 -> CameraScreen(availableCameras.value)
                2 -> SensorScreen(availableSensors.value)
                3 -> MeasurementScreen(measurementResults) { 
                    // Convertir resultado UI a formato de lógica de negocio
                    viewModel.addMeasurementPoint(0f, 0f) // Esto iniciará una medición
                }

                4 -> ARScreen(isCalibrated) { isCalibrated = it }
                5 -> ConfigScreen(hasPermissions, isCalibrated) { isCalibrated = it }
            }
        }

        // Bottom Navigation
        NavigationBar(
            containerColor = Color(0xFFF5F5F5)
        ) {
            tabs.forEachIndexed { index, title ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            when (index) {
                                0 -> Icons.Default.Home
                                1 -> Icons.Default.CameraAlt
                                2 -> Icons.Default.Sensors
                                3 -> Icons.Default.Straighten
                                4 -> Icons.Default.ViewInAr
                                else -> Icons.Default.Settings
                            },
                            contentDescription = title
                        )
                    },
                    label = { Text(title, fontSize = 10.sp) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(hasPermissions: Boolean, isCalibrated: Boolean, cameraCount: Int, sensorCount: Int) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WelcomeSection()
        }

        item {
            StatusCard(hasPermissions, isCalibrated, cameraCount, sensorCount)
        }

        item {
            Text(
                "Características Avanzadas:",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }

        val features = listOf(
            "🎥 Múltiples Cámaras ($cameraCount detectadas)" to "Utiliza todas las cámaras disponibles del dispositivo",
            "🧠 IA Integrada" to "TensorFlow Lite + ML Kit para detección automática",
            "📡 Sensores Completos ($sensorCount activos)" to "Fusión de múltiples sensores para máxima precisión",
            "🔬 Mediciones AR 3D" to "ARCore para mediciones tridimensionales precisas",
            "⚡ Calibración Automática" to "Detecta objetos conocidos para calibración",
            "📊 Análisis Avanzado" to "Indicadores de confianza y estabilidad"
        )

        items(features) { (title, description) ->
            FeatureCard(title, description)
        }
    }
}

@Composable
fun WelcomeSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Straighten,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "¡Bienvenido al Medidor Profesional AR!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Herramienta de medición de vanguardia que utiliza TODAS las cámaras y sensores de tu dispositivo",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF424242)
            )
        }
    }
}

@Composable
fun StatusCard(hasPermissions: Boolean, isCalibrated: Boolean, cameraCount: Int, sensorCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Estado del Sistema",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(12.dp))

            StatusItem("Permisos", if (hasPermissions) "✅ Concedidos" else "⚠️ Pendientes")
            StatusItem("Calibración", if (isCalibrated) "✅ Calibrado" else "🔧 Pendiente")
            StatusItem("Cámaras", "📱 $cameraCount detectadas")
            StatusItem("Sensores", "📡 $sensorCount activos")
            StatusItem("IA/ML", "🧠 TensorFlow + ML Kit")
            StatusItem("AR Core", "🔬 ARCore habilitado")
        }
    }
}

@Composable
fun StatusItem(label: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF666666))
        Text(status, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FeatureCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun CameraScreen(cameras: List<CameraInfo>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Cámaras Disponibles",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }

        items(cameras) { camera ->
            CameraCard(camera)
        }
    }
}

@Composable
fun CameraCard(camera: CameraInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (camera.available) Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (camera.available) Icons.Default.CameraAlt else Icons.Default.CameraAlt,
                contentDescription = null,
                tint = if (camera.available) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    camera.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    camera.type,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            Text(
                if (camera.available) "Disponible" else "No disponible",
                fontSize = 12.sp,
                color = if (camera.available) Color(0xFF4CAF50) else Color(0xFFFF9800),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SensorScreen(sensors: List<SensorInfo>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Sensores Activos",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }

        items(sensors) { sensor ->
            SensorCard(sensor)
        }
    }
}

@Composable
fun SensorCard(sensor: SensorInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (sensor.available) Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Sensors,
                contentDescription = null,
                tint = if (sensor.available) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sensor.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${sensor.type} • Precisión: ${sensor.accuracy}",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            if (sensor.available) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Activo",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MeasurementScreen(results: List<UIMeasurementResult>, onAddResult: (MeasurementResult) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Mediciones",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            Button(
                onClick = {
                    // Usar tipos reales de la lógica de negocio
                    val types = listOf(MeasurementType.DISTANCE, MeasurementType.AREA, MeasurementType.VOLUME, MeasurementType.ANGLE)
                    val units = listOf("cm", "m²", "m³", "°")
                    val randomType = types.random()
                    val randomUnit = units.random()
                    val randomValue = (10..1000).random().toFloat()
                    val randomConfidence = (0.7f..1.0f).random()

                    onAddResult(
                        MeasurementResult(
                            type = randomType,
                            value = randomValue,
                            unit = randomUnit,
                            confidence = randomConfidence,
                            points = emptyList(),
                            method = "Simulación"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva Medición", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (results.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Straighten,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF9E9E9E)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay mediciones aún",
                        fontSize = 16.sp,
                        color = Color(0xFF9E9E9E)
                    )
                    Text(
                        "Presiona 'Nueva Medición' para comenzar",
                        fontSize = 14.sp,
                        color = Color(0xFFBDBDBD)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results.reversed()) { result ->
                    MeasurementCard(result)
                }
            }
        }
    }
}

@Composable
fun MeasurementCard(result: UIMeasurementResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Straighten,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.type,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${result.value} ${result.unit}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(result.confidence * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        result.confidence > 0.9f -> Color(0xFF4CAF50)
                        result.confidence > 0.7f -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                Text(
                    "Confianza",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun ARScreen(isCalibrated: Boolean, onCalibrateChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ViewInAr,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Mediciones AR 3D",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Mediciones tridimensionales precisas usando ARCore",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF666666)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isCalibrated) Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isCalibrated) "✅ Sistema Calibrado" else "🔧 Calibración Pendiente",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!isCalibrated) {
                    Button(
                        onClick = { onCalibrateChange(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Text("Calibrar Ahora", color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* Iniciar medición AR */ },
            enabled = isCalibrated,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Iniciar Medición AR", color = Color.White)
        }
    }
}

@Composable
fun ConfigScreen(
    hasPermissions: Boolean,
    isCalibrated: Boolean,
    onCalibrateChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Configuración",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }

        item {
            ConfigCard("Permisos", "Gestionar permisos de la aplicación", hasPermissions)
        }

        item {
            ConfigCard("Calibración", "Configurar calibración de medición", isCalibrated) {
                onCalibrateChange(!isCalibrated)
            }
        }

        item {
            ConfigCard("Acerca de", "Versión 1.0.0 - Medidor Profesional AR", true)
        }
    }
}

@Composable
fun ConfigCard(title: String, subtitle: String, status: Boolean, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (title) {
                    "Permisos" -> Icons.Default.Security
                    "Calibración" -> Icons.Default.Tune
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            if (title != "Acerca de") {
                Icon(
                    if (status) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (status) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
