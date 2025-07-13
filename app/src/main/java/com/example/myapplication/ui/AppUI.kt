package com.example.myapplication.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.viewmodel.MeasurementViewModel

@Composable
fun AppUI(
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                        selected = false, // TODO: Link to nav state
                        onClick = {
                            navController.navigate(tab.route)
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
            composable("config") { ConfigScreen(viewModel) }
        }
    }
}

private data class TabItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
