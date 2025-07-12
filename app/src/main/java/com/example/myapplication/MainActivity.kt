package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MeasurementAppBasic()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementAppBasic() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Inicio", "Cámara", "Mediciones", "Configuración")
    
    Column(modifier = Modifier.fillMaxSize()) {
        // App Bar
        TopAppBar(
            title = { 
                Text(
                    "Medidor Profesional AR",
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> CameraScreen()
                2 -> MeasurementsScreen()
                3 -> SettingsScreen()
            }
        }
        
        // Bottom Navigation
        NavigationBar {
            tabs.forEachIndexed { index, title ->
                NavigationBarItem(
                    icon = { 
                        Icon(
                            when (index) {
                                0 -> Icons.Default.Home
                                1 -> Icons.Default.PhotoCamera
                                2 -> Icons.Default.Analytics
                                else -> Icons.Default.Settings
                            },
                            contentDescription = title
                        )
                    },
                    label = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }
    }
}

@Composable
fun HomeScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "¡Bienvenido!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Medidor Profesional AR - Herramienta de medición avanzada",
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        item {
            Text(
                "Características Principales:",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            FeatureCard(
                icon = Icons.Default.PhotoCamera,
                title = "Múltiples Cámaras",
                description = "Utiliza todas las cámaras disponibles del dispositivo"
            )
        }
        
        item {
            FeatureCard(
                icon = Icons.Default.Analytics,
                title = "IA Integrada",
                description = "Detección automática de objetos y calibración inteligente"
            )
        }
        
        item {
            FeatureCard(
                icon = Icons.Default.Architecture,
                title = "Mediciones AR",
                description = "Mediciones 3D precisas usando realidad aumentada"
            )
        }
        
        item {
            FeatureCard(
                icon = Icons.Default.Sensors,
                title = "Sensores Avanzados",
                description = "Fusión de múltiples sensores para máxima precisión"
            )
        }
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CameraScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Cámara",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Funcionalidad de cámara próximamente",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MeasurementsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Analytics,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Mediciones",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Historial de mediciones próximamente",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Configuración",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        item {
            SettingItem(
                icon = Icons.Default.Info,
                title = "Acerca de",
                subtitle = "Información de la aplicación"
            )
        }
        
        item {
            SettingItem(
                icon = Icons.Default.Security,
                title = "Permisos",
                subtitle = "Gestionar permisos de la aplicación"
            )
        }
        
        item {
            SettingItem(
                icon = Icons.Default.Tune,
                title = "Calibración",
                subtitle = "Configurar calibración de medición"
            )
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}