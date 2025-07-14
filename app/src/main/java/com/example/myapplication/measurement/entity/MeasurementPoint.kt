package com.example.myapplication.measurement.entity

/**
 * Representa un punto de medición en el espacio.
 * 
 * @property x Coordenada X del punto
 * @property y Coordenada Y del punto
 * @property z Coordenada Z del punto (opcional para mediciones 3D)
 * @property label Etiqueta opcional para el punto
 */
data class MeasurementPoint(
    val x: Float,
    val y: Float,
    val z: Float? = null,
    val label: String? = null
)
