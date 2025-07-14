package com.example.myapplication.measurement.entity

/**
 * Tipos de medición soportados por la aplicación.
 */
enum class MeasurementType {
    /**
     * Medición de distancia entre dos puntos.
     */
    DISTANCE,
    
    /**
     * Medición de área de una superficie.
     */
    AREA,
    
    /**
     * Medición de volumen de un objeto.
     */
    VOLUME,
    
    /**
     * Medición de ángulo entre líneas o superficies.
     */
    ANGLE
}
