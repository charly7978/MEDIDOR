package com.example.myapplication.measurement.entity

/**
 * Constantes matemáticas y físicas con alta precisión para cálculos de medición.
 * Todas las constantes están en unidades del SI.
 */
object MeasurementConstants {
    // Constantes matemáticas
    const val PI = 3.14159265358979323846
    const val PI_2 = 1.57079632679489661923  // π/2
    const val TAU = 6.28318530717958647692   // 2π
    const val DEG_TO_RAD = PI / 180.0        // Conversión de grados a radianes
    const val RAD_TO_DEG = 180.0 / PI        // Conversión de radianes a grados

    // Constantes físicas
    const val EARTH_RADIUS = 6_371_000.0     // Radio ecuatorial de la Tierra en metros
    const val GRAVITY = 9.80665              // Aceleración estándar de la gravedad en m/s²
    const val SPEED_OF_LIGHT = 299_792_458.0  // Velocidad de la luz en m/s
    
    // Constantes de precisión
    const val EPSILON = 1e-10                // Tolerancia para comparaciones de punto flotante
    const val MAX_MEASUREMENT_DISTANCE = 100.0 // Distancia máxima de medición en metros
    
    // Constantes de calibración
    const val MIN_CALIBRATION_DISTANCE = 0.1  // Distancia mínima para calibración en metros
    const val MAX_CALIBRATION_DISTANCE = 10.0 // Distancia máxima para calibración en metros
    
    // Tamaño de caché para resultados intermedios
    const val CACHE_SIZE = 100
    
    // Umbral de confianza para mediciones
    const val CONFIDENCE_THRESHOLD = 0.95
    
    // Valor de confianza por defecto
    const val DEFAULT_CONFIDENCE = 0.9
}
