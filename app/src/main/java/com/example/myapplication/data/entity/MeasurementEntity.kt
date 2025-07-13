package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.graphics.PointF
import androidx.room.TypeConverters
import com.example.myapplication.data.Converters

/**
 * Entidad que representa una medición en la base de datos.
 */
@Entity(tableName = "measurements")
@TypeConverters(Converters::class)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    val type: String,
    val value: Double,
    val unit: String,
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val points: List<PointF> = emptyList(),
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val isFavorite: Boolean = false,
    val isSynced: Boolean = false,
    val calibrationFactor: Double = 1.0,
    val deviceId: String = "",
    val sessionId: String? = null
)