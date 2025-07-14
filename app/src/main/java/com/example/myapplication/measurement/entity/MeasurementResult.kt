package com.example.myapplication.measurement.entity

import com.example.myapplication.camera.MeasurementMethod

/**
 * Modelo de dominio para representar un resultado de medición.
 */
data class MeasurementResult(
    val id: Long = 0,
    val type: MeasurementType,
    val value: Float,
    val unit: String,
    val confidence: Float,
    val points: List<MeasurementPoint> = emptyList(),
    val method: MeasurementMethod,
    val timestamp: Long
)
