package com.example.myapplication.measurement.entity

import java.text.SimpleDateFormat
import java.util.*

/**
 * Representa el resultado de una medición con metadatos asociados.
 * @property type Tipo de medición realizada
 * @property value Valor numérico de la medición
 * @property unit Unidad de medida (m, m², m³, °, etc.)
 * @property confidence Nivel de confianza de la medición (0.0 a 1.0)
 * @property points Puntos utilizados para la medición
 * @property method Método o algoritmo utilizado para la medición
 * @property timestamp Marca de tiempo de la medición (ms desde la época)
 * @property metadata Metadatos adicionales de la medición
 */
data class MeasurementResult(
    val type: MeasurementType,
    val value: Double,
    val unit: String = "m",
    val confidence: Double = 1.0,
    val points: List<MeasurementPoint> = emptyList(),
    val method: String = "default",
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Crea una copia de este resultado con nuevos valores.
     */
    fun copy(
        type: MeasurementType = this.type,
        value: Double = this.value,
        unit: String = this.unit,
        confidence: Double = this.confidence,
        points: List<MeasurementPoint> = this.points,
        method: String = this.method,
        timestamp: Long = this.timestamp,
        metadata: Map<String, Any> = this.metadata
    ) = MeasurementResult(type, value, unit, confidence, points, method, timestamp, metadata)

    /**
     * Convierte el valor a otra unidad compatible.
     * @param targetUnit Unidad de destino (m, cm, mm, km, in, ft, yd, mi)
     * @return Nuevo MeasurementResult con el valor convertido
     */
    fun convertTo(targetUnit: String): MeasurementResult {
        if (targetUnit == unit) return this
        
        val conversionRate = when ("$unit to $targetUnit") {
            // Longitud
            "m to cm" -> 100.0
            "m to mm" -> 1000.0
            "m to km" -> 0.001
            "m to in" -> 39.3701
            "m to ft" -> 3.28084
            "m to yd" -> 1.09361
            "m to mi" -> 0.000621371
            
            // Área
            "m² to cm²" -> 10_000.0
            "m² to mm²" -> 1_000_000.0
            "m² to km²" -> 0.000001
            "m² to ft²" -> 10.7639
            "m² to in²" -> 1550.0
            "m² to acres" -> 0.000247105
            "m² to hectares" -> 0.0001
            
            // Volumen
            "m³ to cm³" -> 1_000_000.0
            "m³ to mm³" -> 1_000_000_000.0
            "m³ to L" -> 1000.0
            "m³ to mL" -> 1_000_000.0
            "m³ to gal" -> 264.172
            "m³ to qt" -> 1056.69
            "m³ to pt" -> 2113.38
            "m³ to cup" -> 4226.75
            "m³ to fl oz" -> 33814.0
            
            // Ángulo (solo para compatibilidad)
            "° to rad" -> 0.0174533
            "rad to °" -> 57.2958
            
            // Conversión inversa
            "cm to m" -> 0.01
            "mm to m" -> 0.001
            "km to m" -> 1000.0
            "in to m" -> 0.0254
            "ft to m" -> 0.3048
            "yd to m" -> 0.9144
            "mi to m" -> 1609.34
            
            "cm² to m²" -> 0.0001
            "mm² to m²" -> 0.000001
            "km² to m²" -> 1_000_000.0
            "ft² to m²" -> 0.092903
            "in² to m²" -> 0.00064516
            "acres to m²" -> 4046.86
            "hectares to m²" -> 10_000.0
            
            "cm³ to m³" -> 0.000001
            "mm³ to m³" -> 0.000000001
            "L to m³" -> 0.001
            "mL to m³" -> 0.000001
            "gal to m³" -> 0.00378541
            "qt to m³" -> 0.000946353
            "pt to m³" -> 0.000473176
            "cup to m³" -> 0.000236588
            "fl oz to m³" -> 2.95735e-5
            
            else -> throw IllegalArgumentException("Conversión no soportada de $unit a $targetUnit")
        }
        
        return copy(
            value = value * conversionRate,
            unit = targetUnit,
            // Reducir ligeramente la confianza por la conversión
            confidence = (confidence * 0.99).coerceAtLeast(0.0)
        )
    }
    
    /**
     * Formatea el valor con la unidad para mostrar al usuario.
     */
    fun formatValue(digits: Int = 2): String {
        return "%.${digits}f".format(value) + " $unit"
    }
    
    /**
     * Obtiene la fecha y hora legible de la medición.
     */
    fun getFormattedDate(): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }
}
