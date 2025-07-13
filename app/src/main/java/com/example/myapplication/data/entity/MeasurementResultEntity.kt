package com.example.myapplication.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad que representa un resultado de medición en la base de datos.
 */
@Entity(
    tableName = "measurement_results",
    foreignKeys = [
        ForeignKey(
            entity = MeasurementEntity::class,
            parentColumns = ["id"],
            childColumns = ["measurementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("measurementId")]
)
data class MeasurementResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val measurementId: Long,
    val value: Float, // Valor de la medición
    val unit: String, // Unidad de medida (cm, m, m², etc.)
    val confidence: Float, // Nivel de confianza (0-1)
    val createdAt: Date,
    val metadata: String? = null, // Metadatos adicionales en formato JSON
    val pointsData: String? = null // Datos de los puntos de medición en formato JSON
)
