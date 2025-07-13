package com.example.myapplication.measurement.entity

/**
 * Representa un punto en el espacio 2D o 3D con información de confianza y metadatos.
 *
 * @property x Coordenada X
 * @property y Coordenada Y
 * @property z Coordenada Z (opcional, 0.0 por defecto para 2D)
 * @property confidence Nivel de confianza del punto (0.0 a 1.0)
 * @property timestamp Marca de tiempo de la medición
 * @property sensorData Datos adicionales de sensores asociados al punto
 */
data class MeasurementPoint(
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
    val confidence: Double = 1.0,
    val timestamp: Long = System.currentTimeMillis(),
    val sensorData: Map<String, Any> = emptyMap()
) {
    init {
        validate()
    }
    
    /**
     * Valida que el punto de medición sea válido.
     * @throws MeasurementError si el punto no es válido
     */
    fun validate() {
        if (x.isNaN() || y.isNaN() || z.isNaN()) {
            throw MeasurementError.InvalidPoint(this, "Las coordenadas no pueden ser NaN")
        }
        if (x.isInfinite() || y.isInfinite() || z.isInfinite()) {
            throw MeasurementError.InvalidPoint(this, "Las coordenadas no pueden ser infinitas")
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw MeasurementError.InvalidPoint(this, "La confianza debe estar entre 0.0 y 1.0")
        }
    }
    
    /**
     * Calcula la distancia euclidiana a otro punto.
     */
    fun distanceTo(other: MeasurementPoint): Double {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Crea una copia del punto con nuevos valores.
     */
    fun copy(
        x: Double = this.x,
        y: Double = this.y,
        z: Double = this.z,
        confidence: Double = this.confidence,
        timestamp: Long = this.timestamp,
        sensorData: Map<String, Any> = this.sensorData
    ): MeasurementPoint = MeasurementPoint(x, y, z, confidence, timestamp, sensorData)
    
    companion object {
        /**
         * Crea un punto de medición a partir de coordenadas 2D.
         */
        fun from2D(x: Double, y: Double, confidence: Double = 1.0): MeasurementPoint {
            return MeasurementPoint(x, y, 0.0, confidence)
        }
        
        /**
         * Crea un punto de medición a partir de coordenadas 3D.
         */
        fun from3D(x: Double, y: Double, z: Double, confidence: Double = 1.0): MeasurementPoint {
            return MeasurementPoint(x, y, z, confidence)
        }
    }
}
