package com.example.myapplication.data.model

import java.util.Date

/**
 * Modelo de dominio para representar un resultado de medición.
 */
data class MeasurementResult(
    val id: Long = 0,
    val measurementId: Long,
    val value: Float,
    val unit: String,
    val confidence: Float,
    val createdAt: Date,
    val points: List<MeasurementPoint> = emptyList(),
    val metadata: Map<String, Any>? = null
)
