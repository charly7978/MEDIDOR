package com.example.myapplication.measurement

/**
 * Jerarquía de errores para el sistema de medición.
 */
sealed class MeasurementError : Exception() {
    // Errores de calibración
    object NotCalibrated : MeasurementError()
    data class CalibrationFailed(val reason: String) : MeasurementError()
    data class InvalidCalibrationDistance(val distance: Double, val min: Double, val max: Double) : MeasurementError()
    
    // Errores de puntos de medición
    data class InvalidPoint(val point: MeasurementPoint, val reason: String) : MeasurementError()
    data class InsufficientPoints(val expected: Int, val actual: Int) : MeasurementError()
    
    // Errores de precisión
    data class InsufficientPrecision(val confidence: Double, val required: Double) : MeasurementError()
    data class MeasurementOutOfRange(val value: Double, val min: Double, val max: Double) : MeasurementError()
    
    // Errores de cámara y sensores
    data class CameraError(val message: String) : MeasurementError()
    data class SensorError(val message: String) : MeasurementError()
    
    // Errores de procesamiento
    data class ProcessingError(val message: String) : MeasurementError()
    
    override fun toString(): String = when (this) {
        is NotCalibrated -> "El sistema no está calibrado. Realice la calibración antes de medir."
        is CalibrationFailed -> "Error en la calibración: $reason"
        is InvalidCalibrationDistance -> "Distancia de calibración $distance m fuera de rango. Debe estar entre $min m y $max m"
        is InvalidPoint -> "Punto no válido en (${point.x}, ${point.y}): $reason"
        is InsufficientPoints -> "Se requieren al menos $expected puntos, pero solo se proporcionaron $actual"
        is InsufficientPrecision -> "Precisión insuficiente: ${(confidence * 100).toInt()}% (mínimo requerido: ${(required * 100).toInt()}%)"
        is MeasurementOutOfRange -> "Valor $value fuera de rango. Debe estar entre $min y $max"
        is CameraError -> "Error de cámara: $message"
        is SensorError -> "Error de sensor: $message"
        is ProcessingError -> "Error de procesamiento: $message"
    }
}

/**
 * Valida un punto de medición.
 * @throws MeasurementError si el punto no es válido
 */
fun MeasurementPoint.validate() {
    if (x.isNaN() || y.isNaN() || z.isNaN()) {
        throw MeasurementError.InvalidPoint(this, "Coordenadas no pueden ser NaN")
    }
    if (x.isInfinite() || y.isInfinite() || z.isInfinite()) {
        throw MeasurementError.InvalidPoint(this, "Coordenadas no pueden ser infinitas")
    }
    if (confidence < 0.0 || confidence > 1.0) {
        throw MeasurementError.InvalidPoint(this, "La confianza debe estar entre 0.0 y 1.0")
    }
}
