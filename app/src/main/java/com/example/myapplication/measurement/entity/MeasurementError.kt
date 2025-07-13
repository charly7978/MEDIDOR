package com.example.myapplication.measurement.entity

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
    data class InvalidMeasurementPoints(val message: String) : MeasurementError()
    
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
        is InvalidPoint -> "Punto de medición inválido: $reason"
        is InsufficientPoints -> "No hay suficientes puntos para la medición. Se requieren $expected pero solo hay $actual"
        is InvalidMeasurementPoints -> "Puntos de medición inválidos: $message"
        is InsufficientPrecision -> "Precisión insuficiente: $confidence (mínimo requerido: $required)"
        is MeasurementOutOfRange -> "Valor $value fuera de rango. Debe estar entre $min y $max"
        is CameraError -> "Error de cámara: $message"
        is SensorError -> "Error de sensor: $message"
        is ProcessingError -> "Error de procesamiento: $message"
    }
}
