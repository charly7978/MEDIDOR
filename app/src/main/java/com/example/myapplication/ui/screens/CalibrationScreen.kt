package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.viewmodel.MeasurementViewModel

@Composable
fun CalibrationScreen(
    viewModel: MeasurementViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var step by remember { mutableStateOf(0) }
    val steps = listOf(
        "Coloca el dispositivo sobre una superficie estable.",
        "No muevas el dispositivo durante la calibración.",
        "Presiona 'Iniciar calibración' para comenzar.",
        "¡Listo! El sistema está calibrado."
    )
    val isLastStep = step == steps.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Calibración Guiada", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(steps[step], style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))
        if (step == 2) {
            Button(
                onClick = {
                    viewModel.setCalibrated(true)
                    step++
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Iniciar calibración", color = MaterialTheme.colorScheme.onPrimary)
            }
        } else if (!isLastStep) {
            Button(
                onClick = { step++ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Siguiente", color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            Text("El sistema está calibrado y listo para usar.", color = MaterialTheme.colorScheme.primary)
        }
    }
} 