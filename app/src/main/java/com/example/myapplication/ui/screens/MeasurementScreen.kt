package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.measurement.MeasurementResult
import com.example.myapplication.measurement.MeasurementType
import com.example.myapplication.viewmodel.MeasurementViewModel

@Composable
fun MeasurementScreen(
    viewModel: MeasurementViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
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
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = {
                    // Simular nueva medición
                    val types = listOf(MeasurementType.DISTANCE, MeasurementType.AREA, MeasurementType.VOLUME, MeasurementType.ANGLE)
                    val units = listOf("cm", "m²", "m³", "°")
                    val randomType = types.random()
                    val randomUnit = units.random()
                    val randomValue = (10..1000).random().toFloat()
                    val randomConfidence = (0.7f..1.0f).random()

                    val newResult = MeasurementResult(
                        type = randomType,
                        value = randomValue,
                        unit = randomUnit,
                        confidence = randomConfidence,
                        points = emptyList(),
                        method = "Simulación"
                    )
                    viewModel.addMeasurementResult(newResult)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva Medición", color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.measurementResults.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Straighten,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay mediciones aún",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Presiona 'Nueva Medición' para comenzar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.measurementResults.reversed()) { result ->
                    MeasurementCard(result)
                }
            }
        }
    }
}

@Composable
private fun MeasurementCard(result: MeasurementResult) {
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.type.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${result.value} ${result.unit}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(result.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        result.confidence > 0.9f -> MaterialTheme.colorScheme.primary
                        result.confidence > 0.7f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    "Confianza",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 