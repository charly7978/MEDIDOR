package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.components.FeatureCard
import com.example.myapplication.ui.components.StatusCard
import com.example.myapplication.viewmodel.MeasurementViewModel

@Composable
fun HomeScreen(
    viewModel: MeasurementViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WelcomeSection()
        }

        item {
            StatusCard(
                hasPermissions = state.hasPermissions,
                isCalibrated = state.isCalibrated,
                cameraCount = state.availableCameras.size,
                sensorCount = state.availableSensors.size
            )
        }

        item {
            Text(
                text = "Características avanzadas:",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(state.features) { feature ->
            FeatureCard(
                title = feature.title,
                description = feature.description
            )
        }
    }
}

@Composable
private fun WelcomeSection() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "¡Bienvenido al Medidor Profesional AR!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Herramienta de medición de vanguardia que utiliza todas las cámaras y sensores de tu dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
