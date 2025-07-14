package com.example.myapplication.camera

/**
 * Métodos de medición soportados por la aplicación.
 */
enum class MeasurementMethod {
    /**
     * Medición basada en visión estéreo.
     */
    STEREO_VISION,
    
    /**
     * Medición basada en sensor de profundidad.
     */
    DEPTH_SENSOR,
    
    /**
     * Medición basada en distancia focal.
     */
    FOCAL_LENGTH,
    
    /**
     * Medición basada en ARCore.
     */
    AR_CORE
}
