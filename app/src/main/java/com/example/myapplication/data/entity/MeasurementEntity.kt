package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad que representa una medición en la base de datos.
 */
@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Date,
    val updatedAt: Date,
    val type: String, // Tipo de medición (distancia, área, volumen, etc.)
    val calibrationFactor: Float, // Factor de calibración utilizado
    val isArchived: Boolean = false
)
