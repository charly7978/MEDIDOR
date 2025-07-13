package com.example.myapplication.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.R

@Composable
fun StatusCard(
    hasPermissions: Boolean,
    isCalibrated: Boolean,
    cameraCount: Int,
    sensorCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.status_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            StatusItem(
                label = stringResource(R.string.permissions_label),
                status = if (hasPermissions) stringResource(R.string.permissions_granted) 
                        else stringResource(R.string.permissions_pending)
            )
            StatusItem(
                label = stringResource(R.string.calibration_label),
                status = if (isCalibrated) stringResource(R.string.calibration_calibrated)
                        else stringResource(R.string.calibration_pending)
            )
            StatusItem(
                label = stringResource(R.string.cameras_label),
                status = stringResource(R.string.cameras_count, cameraCount)
            )
            StatusItem(
                label = stringResource(R.string.sensors_label),
                status = stringResource(R.string.sensors_count, sensorCount)
            )
        }
    }
}

@Composable
private fun StatusItem(label: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
