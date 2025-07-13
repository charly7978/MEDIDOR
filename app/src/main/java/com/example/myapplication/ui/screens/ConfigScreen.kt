package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.viewmodel.MeasurementViewModel
import androidx.navigation.NavController

@Composable
fun ConfigScreen(
    viewModel: MeasurementViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Configuración",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            ConfigCard(
                title = "Permisos",
                subtitle = "Gestionar permisos de la aplicación",
                status = state.hasPermissions
            )
        }

        item {
            ConfigCard(
                title = "Autodiagnóstico",
                subtitle = "Verifica el estado de sensores y cámaras",
                status = state.isCalibrated
            ) {
                navController.navigate("diagnostic")
            }
        }
        item {
            ConfigCard(
                title = "Calibración Guiada",
                subtitle = "Asistente paso a paso para calibrar sensores y cámaras",
                status = state.isCalibrated
            ) {
                navController.navigate("calibration")
            }
        }

        item {
            ConfigCard(
                title = "Acerca de",
                subtitle = "Versión 1.0.0 - Medidor Profesional AR",
                status = true
            )
        }
    }
}

@Composable
private fun ConfigCard(
    title: String,
    subtitle: String,
    status: Boolean,
    onClick: (() -> Unit)? = null
) {
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (title != "Acerca de") {
                Icon(
                    if (status) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
} 