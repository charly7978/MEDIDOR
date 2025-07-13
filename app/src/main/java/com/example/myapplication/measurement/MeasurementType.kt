package com.example.myapplication.measurement

/**
 * Enumeración que representa los diferentes tipos de mediciones que puede realizar la aplicación.
 */
enum class MeasurementType {
    /**
     * Medición de distancia entre dos puntos.
     */
    DISTANCE,
    
    /**
     * Medición de área de un polígono definido por múltiples puntos.
     */
    AREA,
    
    /**
     * Medición de volumen de un objeto tridimensional.
     */
    VOLUME,
    
    /**
     * Medición de ángulo entre tres puntos.
     */
    ANGLE,
    
    /**
     * Medición de altura de un objeto.
     */
    HEIGHT,
    
    /**
     * Medición de ancho de un objeto.
     */
    WIDTH,
    
    /**
     * Medición de profundidad de un objeto.
     */
    DEPTH,
    
    /**
     * Medición de perímetro de una figura.
     */
    PERIMETER,
    
    /**
     * Medición de radio de un círculo o arco.
     */
    RADIUS,
    
    /**
     * Medición de diámetro de un círculo.
     */
    DIAMETER;
    
    /**
     * Obtiene la unidad de medida predeterminada para este tipo de medición.
     */
    val defaultUnit: String
        get() = when (this) {
            DISTANCE, HEIGHT, WIDTH, DEPTH, RADIUS, DIAMETER -> "m"
            AREA -> "m²"
            VOLUME -> "m³"
            ANGLE -> "°"
            PERIMETER -> "m"
        }
    
    /**
     * Verifica si este tipo de medición es compatible con la unidad especificada.
     */
    fun isCompatibleWith(unit: String): Boolean {
        return when (this) {
            DISTANCE, HEIGHT, WIDTH, DEPTH, RADIUS, DIAMETER, PERIMETER -> 
                unit in listOf("m", "cm", "mm", "km", "in", "ft", "yd", "mi")
            AREA -> 
                unit in listOf("m²", "cm²", "mm²", "km²", "in²", "ft²", "yd²", "mi²", "ha", "ac")
            VOLUME -> 
                unit in listOf("m³", "cm³", "mm³", "L", "mL", "gal", "qt", "pt", "fl oz")
            ANGLE -> 
                unit in listOf("°", "rad", "grad")
        }
    }
    
    /**
     * Obtiene el factor de conversión desde la unidad predeterminada a la unidad especificada.
     */
    fun getConversionFactor(unit: String): Double {
        return when (this) {
            DISTANCE, HEIGHT, WIDTH, DEPTH, RADIUS, DIAMETER, PERIMETER -> 
                when (unit) {
                    "m" -> 1.0
                    "cm" -> 100.0
                    "mm" -> 1000.0
                    "km" -> 0.001
                    "in" -> 39.3701
                    "ft" -> 3.28084
                    "yd" -> 1.09361
                    "mi" -> 0.000621371
                    else -> 1.0
                }
            AREA -> 
                when (unit) {
                    "m²" -> 1.0
                    "cm²" -> 10000.0
                    "mm²" -> 1000000.0
                    "km²" -> 0.000001
                    "in²" -> 1550.0
                    "ft²" -> 10.7639
                    "yd²" -> 1.19599
                    "mi²" -> 3.86102e-7
                    "ha" -> 0.0001
                    "ac" -> 0.000247105
                    else -> 1.0
                }
            VOLUME -> 
                when (unit) {
                    "m³" -> 1.0
                    "cm³" -> 1000000.0
                    "mm³" -> 1e+9
                    "L" -> 1000.0
                    "mL" -> 1000000.0
                    "gal" -> 264.172
                    "qt" -> 1056.69
                    "pt" -> 2113.38
                    "fl oz" -> 33814.0
                    else -> 1.0
                }
            ANGLE -> 
                when (unit) {
                    "°" -> 1.0
                    "rad" -> 0.0174533
                    "grad" -> 1.11111
                    else -> 1.0
                }
        }
    }
}
