package com.example.myapplication.measurement.entity

/**
 * Clave para el caché de mediciones.
 * 
 * @property type Tipo de medición
 * @property pointsHash Hash de los puntos de medición
 * @property method Método de medición utilizado
 */
data class MeasurementCacheKey(
    val type: MeasurementType,
    val pointsHash: Int,
    val method: String
)
