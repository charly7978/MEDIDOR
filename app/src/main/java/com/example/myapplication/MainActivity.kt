package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.screens.MeasurementScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.viewmodel.MeasurementViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MeasurementViewModel = viewModel()
                    
                    // Solicitar permisos necesarios
                    val permissionsState = rememberMultiplePermissionsState(
                        listOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                    
                    LaunchedEffect(Unit) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                    
                    if (permissionsState.allPermissionsGranted) {
                        MeasurementScreen(viewModel = viewModel)
                    } else {
                        PermissionRequiredScreen {
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequiredScreen(onRequestPermissions: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { },
        title = { androidx.compose.material3.Text("Permisos Requeridos") },
        text = { 
            androidx.compose.material3.Text(
                "Esta aplicación necesita acceso a la cámara, ubicación y sensores para funcionar correctamente."
            ) 
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onRequestPermissions) {
                androidx.compose.material3.Text("Conceder Permisos")
            }
        }
    )
}