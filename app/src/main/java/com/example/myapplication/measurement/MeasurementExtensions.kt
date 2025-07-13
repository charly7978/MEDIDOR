package com.example.myapplication.measurement

import android.graphics.PointF
import com.example.myapplication.data.entity.MeasurementEntity
import java.util.*

/**
 * Extensiones para convertir entre diferentes tipos de mediciones.
 */

/**
 * Convierte un MeasurementResult a MeasurementEntity.
 */
fun MeasurementResult.toMeasurementEntity(
    tags: List<String> = emptyList(),
    notes: String = ""
): MeasurementEntity {
    return MeasurementEntity(
        type = this.type.name,
        value = this.value,
        unit = this.unit,
        confidence = this.confidence.toFloat(),
        points = this.points.map { PointF(it.x.toFloat(), it.y.toFloat()) },
        tags = tags,
        notes = notes,
        calibrationFactor = 1.0, // Obtener del motor de medición si es necesario
        method = this.method,
        deviceId = "" // Obtener del dispositivo si es necesario
    )
}

/**
 * Convierte un MeasurementEntity a MeasurementResult.
 */
fun MeasurementEntity.toMeasurementResult(): MeasurementResult {
    return MeasurementResult(
        type = MeasurementType.valueOf(this.type.uppercase(Locale.ROOT)),
        value = this.value,
        unit = this.unit,
        confidence = this.confidence.toDouble(),
        points = this.points.map { point ->
            MeasurementPoint(
                x = point.x.toDouble(),
                y = point.y.toDouble(),
                z = 0.0, // No tenemos información Z en PointF
                confidence = this.confidence.toDouble(),
                timestamp = this.timestamp,
                sensorData = emptyMap() // No tenemos información de sensores guardada
            )
        },
        method = this.method,
        timestamp = this.timestamp
    )
}

/**
 * Convierte un MeasurementResult a MeasurementResultEntity.
 */
fun MeasurementResult.toMeasurementResultEntity(): MeasurementResultEntity {
    return MeasurementResultEntity(
        type = this.type.toString(),
        value = this.value.toFloat(),
        unit = this.unit,
        confidence = this.confidence.toFloat(),
        method = this.method,
        timestamp = this.timestamp
    )
}

/**
 * Convierte un MeasurementResultEntity a MeasurementResult.
 */
fun MeasurementResultEntity.toMeasurementResult(): MeasurementResult {
    return MeasurementResult(
        type = MeasurementType.valueOf(this.type.uppercase(Locale.ROOT)),
        value = this.value.toDouble(),
        unit = this.unit,
        confidence = this.confidence.toDouble(),
        points = emptyList(), // No tenemos información de puntos en MeasurementResultEntity
        method = this.method,
        timestamp = this.timestamp
    )
}
