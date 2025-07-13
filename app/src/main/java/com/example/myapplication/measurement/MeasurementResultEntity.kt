package com.example.myapplication.measurement

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.myapplication.data.Converters

/**
 * Entidad que representa el resultado de una medición en la base de datos.
 * 
 * @property id Identificador único de la medición (autogenerado)
 * @property type Tipo de medición (DISTANCE, AREA, VOLUME, etc.)
 * @property value Valor numérico de la medición
 * @property unit Unidad de medida (m, m², m³, etc.)
 * @param confidence Nivel de confianza de la medición (0.0 a 1.0)
 * @param method Método o algoritmo utilizado para la medición
 * @param timestamp Marca de tiempo de la medición (ms desde la época)
 */
@Entity(tableName = "measurement_results")
@TypeConverters(Converters::class)
data class MeasurementResultEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val type: String,
    val value: Float,
    val unit: String,
    val confidence: Float,
    val method: String,
    val timestamp: Long = System.currentTimeMillis()
)