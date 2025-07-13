package com.example.myapplication.measurement

import kotlin.math.*

/**
 * Utilidad para convertir entre diferentes unidades de medida.
 * Proporciona métodos para convertir entre unidades de longitud, área, volumen y ángulo.
 */
object UnitConverter {
    // Constantes de conversión
    private const val METERS_TO_FEET = 3.28084
    private const val METERS_TO_INCHES = 39.3701
    private const val METERS_TO_CENTIMETERS = 100.0
    private const val METERS_TO_MILLIMETERS = 1000.0
    private const val SQUARE_METERS_TO_SQUARE_FEET = 10.7639
    private const val SQUARE_METERS_TO_SQUARE_INCHES = 1550.0
    private const val CUBIC_METERS_TO_CUBIC_FEET = 35.3147
    private const val CUBIC_METERS_TO_GALLONS = 264.172
    private const val CUBIC_METERS_TO_LITERS = 1000.0
    
    // Precisión para comparaciones de punto flotante
    private const val EPSILON = 1e-10
    
    /**
     * Convierte un valor de una unidad a otra.
     * 
     * @param value Valor a convertir
     * @param fromUnit Unidad de origen
     * @param toUnit Unidad de destino
     * @return Valor convertido, o el valor original si la conversión no es compatible
     */
    fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        if (fromUnit == toUnit) return value
        
        return when {
            // Longitud
            isLengthUnit(fromUnit) && isLengthUnit(toUnit) -> {
                convertLength(value, fromUnit, toUnit)
            }
            // Área
            isAreaUnit(fromUnit) && isAreaUnit(toUnit) -> {
                convertArea(value, fromUnit, toUnit)
            }
            // Volumen
            isVolumeUnit(fromUnit) && isVolumeUnit(toUnit) -> {
                convertVolume(value, fromUnit, toUnit)
            }
            // Ángulo
            isAngleUnit(fromUnit) && isAngleUnit(toUnit) -> {
                convertAngle(value, fromUnit, toUnit)
            }
            // Conversión no soportada
            else -> value
        }
    }
    
    /**
     * Convierte un valor de longitud entre diferentes unidades.
     */
    fun convertLength(value: Double, fromUnit: String, toUnit: String): Double {
        // Primero convertir a metros
        val meters = when (fromUnit.lowercase()) {
            "m", "meter", "meters" -> value
            "cm", "centimeter", "centimeters" -> value / METERS_TO_CENTIMETERS
            "mm", "millimeter", "millimeters" -> value / METERS_TO_MILLIMETERS
            "ft", "foot", "feet" -> value / METERS_TO_FEET
            "in", "inch", "inches" -> value / METERS_TO_INCHES
            "yd", "yard", "yards" -> value / (METERS_TO_FEET / 3.0)
            "mi", "mile", "miles" -> value / (METERS_TO_FEET * 5280.0)
            "km", "kilometer", "kilometers" -> value * 1000.0
            else -> return value // Unidad no reconocida
        }
        
        // Luego convertir a la unidad de destino
        return when (toUnit.lowercase()) {
            "m", "meter", "meters" -> meters
            "cm", "centimeter", "centimeters" -> meters * METERS_TO_CENTIMETERS
            "mm", "millimeter", "millimeters" -> meters * METERS_TO_MILLIMETERS
            "ft", "foot", "feet" -> meters * METERS_TO_FEET
            "in", "inch", "inches" -> meters * METERS_TO_INCHES
            "yd", "yard", "yards" -> meters * (METERS_TO_FEET / 3.0)
            "mi", "mile", "miles" -> meters * (METERS_TO_FEET * 5280.0)
            "km", "kilometer", "kilometers" -> meters / 1000.0
            else -> value // Unidad no reconocida
        }
    }
    
    /**
     * Convierte un valor de área entre diferentes unidades.
     */
    fun convertArea(value: Double, fromUnit: String, toUnit: String): Double {
        // Primero convertir a metros cuadrados
        val squareMeters = when (fromUnit.lowercase()) {
            "m²", "m^2", "sq m", "square meter", "square meters" -> value
            "cm²", "cm^2", "sq cm", "square centimeter", "square centimeters" -> value / (METERS_TO_CENTIMETERS * METERS_TO_CENTIMETERS)
            "mm²", "mm^2", "sq mm", "square millimeter", "square millimeters" -> value / (METERS_TO_MILLIMETERS * METERS_TO_MILLIMETERS)
            "ft²", "ft^2", "sq ft", "square foot", "square feet" -> value / SQUARE_METERS_TO_SQUARE_FEET
            "in²", "in^2", "sq in", "square inch", "square inches" -> value / SQUARE_METERS_TO_SQUARE_INCHES
            "acre", "acres" -> value * 4046.86 // 1 acre = 4046.86 m²
            "hectare", "hectares" -> value * 10000.0 // 1 hectárea = 10,000 m²
            else -> return value // Unidad no reconocida
        }
        
        // Luego convertir a la unidad de destino
        return when (toUnit.lowercase()) {
            "m²", "m^2", "sq m", "square meter", "square meters" -> squareMeters
            "cm²", "cm^2", "sq cm", "square centimeter", "square centimeters" -> squareMeters * METERS_TO_CENTIMETERS * METERS_TO_CENTIMETERS
            "mm²", "mm^2", "sq mm", "square millimeter", "square millimeters" -> squareMeters * METERS_TO_MILLIMETERS * METERS_TO_MILLIMETERS
            "ft²", "ft^2", "sq ft", "square foot", "square feet" -> squareMeters * SQUARE_METERS_TO_SQUARE_FEET
            "in²", "in^2", "sq in", "square inch", "square inches" -> squareMeters * SQUARE_METERS_TO_SQUARE_INCHES
            "acre", "acres" -> squareMeters / 4046.86
            "hectare", "hectares" -> squareMeters / 10000.0
            else -> value // Unidad no reconocida
        }
    }
    
    /**
     * Convierte un valor de volumen entre diferentes unidades.
     */
    fun convertVolume(value: Double, fromUnit: String, toUnit: String): Double {
        // Primero convertir a metros cúbicos
        val cubicMeters = when (fromUnit.lowercase()) {
            "m³", "m^3", "cubic meter", "cubic meters" -> value
            "cm³", "cm^3", "cubic centimeter", "cubic centimeters" -> value / (METERS_TO_CENTIMETERS * METERS_TO_CENTIMETERS * METERS_TO_CENTIMETERS)
            "mm³", "mm^3", "cubic millimeter", "cubic millimeters" -> value / (METERS_TO_MILLIMETERS * METERS_TO_MILLIMETERS * METERS_TO_MILLIMETERS)
            "ft³", "ft^3", "cubic foot", "cubic feet" -> value / CUBIC_METERS_TO_CUBIC_FEET
            "in³", "in^3", "cubic inch", "cubic inches" -> value / (CUBIC_METERS_TO_CUBIC_FEET * 1728.0) // 1 pie cúbico = 1728 pulgadas cúbicas
            "l", "liter", "liters", "litre", "litres" -> value / CUBIC_METERS_TO_LITERS
            "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> value / (CUBIC_METERS_TO_LITERS * 1000.0)
            "gal", "gallon", "gallons" -> value / CUBIC_METERS_TO_GALLONS
            "qt", "quart", "quarts" -> value / (CUBIC_METERS_TO_GALLONS * 4.0)
            "pt", "pint", "pints" -> value / (CUBIC_METERS_TO_GALLONS * 8.0)
            "cup", "cups" -> value / (CUBIC_METERS_TO_GALLONS * 16.0)
            "fl oz", "fluid ounce", "fluid ounces" -> value / (CUBIC_METERS_TO_GALLONS * 128.0)
            else -> return value // Unidad no reconocida
        }
        
        // Luego convertir a la unidad de destino
        return when (toUnit.lowercase()) {
            "m³", "m^3", "cubic meter", "cubic meters" -> cubicMeters
            "cm³", "cm^3", "cubic centimeter", "cubic centimeters" -> cubicMeters * METERS_TO_CENTIMETERS * METERS_TO_CENTIMETERS * METERS_TO_CENTIMETERS
            "mm³", "mm^3", "cubic millimeter", "cubic millimeters" -> cubicMeters * METERS_TO_MILLIMETERS * METERS_TO_MILLIMETERS * METERS_TO_MILLIMETERS
            "ft³", "ft^3", "cubic foot", "cubic feet" -> cubicMeters * CUBIC_METERS_TO_CUBIC_FEET
            "in³", "in^3", "cubic inch", "cubic inches" -> cubicMeters * CUBIC_METERS_TO_CUBIC_FEET * 1728.0
            "l", "liter", "liters", "litre", "litres" -> cubicMeters * CUBIC_METERS_TO_LITERS
            "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> cubicMeters * CUBIC_METERS_TO_LITERS * 1000.0
            "gal", "gallon", "gallons" -> cubicMeters * CUBIC_METERS_TO_GALLONS
            "qt", "quart", "quarts" -> cubicMeters * CUBIC_METERS_TO_GALLONS * 4.0
            "pt", "pint", "pints" -> cubicMeters * CUBIC_METERS_TO_GALLONS * 8.0
            "cup", "cups" -> cubicMeters * CUBIC_METERS_TO_GALLONS * 16.0
            "fl oz", "fluid ounce", "fluid ounces" -> cubicMeters * CUBIC_METERS_TO_GALLONS * 128.0
            else -> value // Unidad no reconocida
        }
    }
    
    /**
     * Convierte un ángulo entre diferentes unidades.
     */
    fun convertAngle(value: Double, fromUnit: String, toUnit: String): Double {
        // Primero convertir a radianes
        val radians = when (fromUnit.lowercase()) {
            "rad", "radian", "radians" -> value
            "deg", "degree", "degrees" -> Math.toRadians(value)
            "grad", "gradian", "gradians", "gon", "gons" -> value * (Math.PI / 200.0) // 200 grados = π radianes
            "rev", "revolution", "revolutions", "turn", "turns", "rot", "rotation", "rotations" -> value * (2.0 * Math.PI)
            else -> return value // Unidad no reconocida
        }
        
        // Luego convertir a la unidad de destino
        return when (toUnit.lowercase()) {
            "rad", "radian", "radians" -> radians
            "deg", "degree", "degrees" -> Math.toDegrees(radians)
            "grad", "gradian", "gradians", "gon", "gons" -> radians * (200.0 / Math.PI)
            "rev", "revolution", "revolutions", "turn", "turns", "rot", "rotation", "rotations" -> radians / (2.0 * Math.PI)
            else -> value // Unidad no reconocida
        }
    }
    
    /**
     * Formatea un valor numérico con el número apropiado de decimales
     * según la magnitud del valor.
     */
    fun formatNumber(value: Double, unit: String): String {
        val absValue = abs(value)
        val formattedValue = when {
            absValue == 0.0 -> "0"
            absValue < 0.001 -> String.format("%.6g", value)
            absValue < 0.01 -> String.format("%.5f", value).trimEnd('0').trimEnd('.')
            absValue < 0.1 -> String.format("%.4f", value).trimEnd('0').trimEnd('.')
            absValue < 1.0 -> String.format("%.3f", value).trimEnd('0').trimEnd('.')
            absValue < 10.0 -> String.format("%.2f", value).trimEnd('0').trimEnd('.')
            absValue < 100.0 -> String.format("%.1f", value).trimEnd('0').trimEnd('.')
            else -> String.format("%.0f", value)
        }
        
        return "$formattedValue $unit"
    }
    
    // Funciones de utilidad para verificar tipos de unidades
    
    fun isLengthUnit(unit: String): Boolean {
        return unit.matches("(?i)(m|meter|meters|cm|centimeter|centimeters|mm|millimeter|millimeters|ft|foot|feet|in|inch|inches|yd|yard|yards|mi|mile|miles|km|kilometer|kilometers)\\b".toRegex())
    }
    
    fun isAreaUnit(unit: String): Boolean {
        return unit.matches("(?i)(m²|m\\^2|sq\\s*m|square\\s*(meter|meters)|cm²|cm\\^2|sq\\s*cm|square\\s*centimeter|square\\s*centimeters|mm²|mm\\^2|sq\\s*mm|square\\s*millimeter|square\\s*millimeters|ft²|ft\\^2|sq\\s*ft|square\\s*foot|square\\s*feet|in²|in\\^2|sq\\s*in|square\\s*inch|square\\s*inches|acre|acres|hectare|hectares)\\b".toRegex())
    }
    
    fun isVolumeUnit(unit: String): Boolean {
        return unit.matches("(?i)(m³|m\\^3|cubic\\s*(meter|meters)|cm³|cm\\^3|cubic\\s*centimeter|cubic\\s*centimeters|mm³|mm\\^3|cubic\\s*millimeter|cubic\\s*millimeters|ft³|ft\\^3|cubic\\s*foot|cubic\\s*feet|in³|in\\^3|cubic\\s*inch|cubic\\s*inches|l|liter|liters|litre|litres|ml|milliliter|milliliters|millilitre|millilitres|gal|gallon|gallons|qt|quart|quarts|pt|pint|pints|cup|cups|fl\\s*oz|fluid\\s*ounce|fluid\\s*ounces)\\b".toRegex())
    }
    
    fun isAngleUnit(unit: String): Boolean {
        return unit.matches("(?i)(rad|radian|radians|deg|degree|degrees|grad|gradian|gradians|gon|gons|rev|revolution|revolutions|turn|turns|rot|rotation|rotations)\\b".toRegex())
    }
    
    /**
     * Obtiene la unidad estándar para un tipo de medición.
     */
    fun getStandardUnit(unitType: String): String {
        return when (unitType.lowercase()) {
            "length" -> "m"
            "area" -> "m²"
            "volume" -> "m³"
            "angle" -> "deg"
            else -> ""
        }
    }
    
    /**
     * Obtiene todas las unidades disponibles para un tipo de medición.
     */
    fun getAvailableUnits(unitType: String): List<String> {
        return when (unitType.lowercase()) {
            "length" -> listOf("m", "cm", "mm", "ft", "in", "yd", "mi", "km")
            "area" -> listOf("m²", "cm²", "mm²", "ft²", "in²", "acre", "hectare")
            "volume" -> listOf("m³", "cm³", "mm³", "ft³", "in³", "l", "ml", "gal", "qt", "pt", "cup", "fl oz")
            "angle" -> listOf("deg", "rad", "grad", "rev")
            else -> emptyList()
        }
    }
    
    /**
     * Obtiene el nombre completo de una unidad a partir de su abreviatura.
     */
    fun getUnitName(unit: String): String {
        return when (unit.lowercase()) {
            // Longitud
            "m" -> "meters"
            "cm" -> "centimeters"
            "mm" -> "millimeters"
            "ft" -> "feet"
            "in" -> "inches"
            "yd" -> "yards"
            "mi" -> "miles"
            "km" -> "kilometers"
            
            // Área
            "m²", "m^2" -> "square meters"
            "cm²", "cm^2" -> "square centimeters"
            "mm²", "mm^2" -> "square millimeters"
            "ft²", "ft^2" -> "square feet"
            "in²", "in^2" -> "square inches"
            "acre" -> "acres"
            "hectare" -> "hectares"
            
            // Volumen
            "m³", "m^3" -> "cubic meters"
            "cm³", "cm^3" -> "cubic centimeters"
            "mm³", "mm^3" -> "cubic millimeters"
            "ft³", "ft^3" -> "cubic feet"
            "in³", "in^3" -> "cubic inches"
            "l" -> "liters"
            "ml" -> "milliliters"
            "gal" -> "gallons"
            "qt" -> "quarts"
            "pt" -> "pints"
            "cup" -> "cups"
            "fl oz" -> "fluid ounces"
            
            // Ángulo
            "deg" -> "degrees"
            "rad" -> "radians"
            "grad" -> "gradians"
            "rev" -> "revolutions"
            
            // Valor por defecto
            else -> unit
        }
    }
}
