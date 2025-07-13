package com.example.myapplication.measurement.entity

/**
 * Representa un objeto detectado por el sistema de visión por computadora.
 * 
 * @property boundingBox Rectángulo delimitador del objeto en la imagen
 * @property confidence Nivel de confianza de la detección (0.0 a 1.0)
 * @property trackingId Identificador único para rastrear el objeto entre frames
 * @property labels Etiquetas o categorías del objeto detectado
 */
data class DetectedObject(
    val boundingBox: android.graphics.Rect,
    val confidence: Float,
    val trackingId: Int?,
    val labels: List<String>
)
