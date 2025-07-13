package com.example.myapplication.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.screens.DiagnosticScreen
import com.example.myapplication.ui.screens.CalibrationScreen
import com.example.myapplication.viewmodel.MeasurementViewModel

@Composable
fun AppUI(
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                {
                    Text("Medidor Profesional AR")
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val tabs = listOf(
                    TabItem("home", Icons.Default.Home, "Inicio"),
                    TabItem("cameras", Icons.Default.CameraAlt, "Cámaras"),
                    TabItem("sensors", Icons.Default.Sensors, "Sensores"),
                    TabItem("measure", Icons.Default.Straighten, "Mediciones"),
                    TabItem("ar", Icons.Default.ViewInAr, "AR"),
                    TabItem("config", Icons.Default.Settings, "Config")
                )

                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(viewModel) }
            composable("cameras") { CameraScreen(viewModel) }
            composable("sensors") { SensorScreen(viewModel) }
            composable("measure") { MeasurementScreen(viewModel) }
            composable("ar") { ARScreen(viewModel) }
            composable("config") { ConfigScreen(viewModel, navController) }
            composable("diagnostic") { DiagnosticScreen(viewModel) }
            composable("calibration") { CalibrationScreen(viewModel) }
        }
    }
}

private data class TabItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
