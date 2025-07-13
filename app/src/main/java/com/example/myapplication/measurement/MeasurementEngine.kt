package com.example.myapplication.measurement

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.layout.Box
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.math.*

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
        return sqrt(dx * dx + dy * dy + dz * dz)
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
    val unit: String,
    val confidence: Double,
    val points: List<MeasurementPoint>,
    val method: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {
    init {
        require(confidence in 0.0..1.0) { "La confianza debe estar entre 0.0 y 1.0" }
        require(value.isFinite()) { "El valor de la medición debe ser finito" }
        require(!value.isNaN()) { "El valor de la medición no puede ser NaN" }
        
        // Validar puntos según el tipo de medición
        when (type) {
            MeasurementType.DISTANCE -> require(points.size == 2) { "Se requieren exactamente 2 puntos para medir distancia" }
            MeasurementType.AREA -> require(points.size >= 3) { "Se requieren al menos 3 puntos para calcular área" }
            MeasurementType.ANGLE -> require(points.size == 3) { "Se requieren exactamente 3 puntos para medir ángulos" }
            MeasurementType.VOLUME -> require(points.isEmpty()) { "No se requieren puntos para medición de volumen directo" }
            else -> { /* No se requieren validaciones específicas */ }
        }
    }
    
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
    ): MeasurementResult = MeasurementResult(type, value, unit, confidence, points, method, timestamp, metadata)
    
    /**
     * Convierte el valor a otra unidad compatible.
     * @param targetUnit Unidad de destino (m, cm, mm, km, in, ft, yd, mi)
     * @return Nuevo MeasurementResult con el valor convertido
     */
    fun convertTo(targetUnit: String): MeasurementResult {
        if (unit == targetUnit) return this
        
        val conversionRate = when (unit to targetUnit) {
            // Longitud
            "m" to "cm" -> 100.0
            "m" to "mm" -> 1000.0
            "m" to "km" -> 0.001
            "m" to "in" -> 39.3701
            "m" to "ft" -> 3.28084
            "m" to "yd" -> 1.09361
            "m" to "mi" -> 0.000621371
            
            // Área
            "m²" to "cm²" -> 10_000.0
            "m²" to "mm²" -> 1_000_000.0
            "m²" to "km²" -> 0.000001
            "m²" to "ft²" -> 10.7639
            "m²" to "in²" -> 1550.0
            "m²" to "acres" -> 0.000247105
            "m²" to "hectares" -> 0.0001
            
            // Volumen
            "m³" to "cm³" -> 1_000_000.0
            "m³" to "mm³" -> 1_000_000_000.0
            "m³" to "L" -> 1000.0
            "m³" to "mL" -> 1_000_000.0
            "m³" to "gal" -> 264.172
            "m³" to "qt" -> 1056.69
            "m³" to "pt" -> 2113.38
            "m³" to "cup" -> 4226.75
            "m³" to "fl oz" -> 33814.0
            
            // Ángulo (solo para compatibilidad)
            "°" to "rad" -> 0.0174533
            "rad" to "°" -> 57.2958
            
            // Conversión inversa
            "cm" to "m" -> 0.01
            "mm" to "m" -> 0.001
            "km" to "m" -> 1000.0
            "in" to "m" -> 0.0254
            "ft" to "m" -> 0.3048
            "yd" to "m" -> 0.9144
            "mi" to "m" -> 1609.34
            
            "cm²" to "m²" -> 0.0001
            "mm²" to "m²" -> 0.000001
            "km²" to "m²" -> 1_000_000.0
            "ft²" to "m²" -> 0.092903
            "in²" to "m²" -> 0.00064516
            "acres" to "m²" -> 4046.86
            "hectares" to "m²" -> 10_000.0
            
            "cm³" to "m³" -> 0.000001
            "mm³" to "m³" -> 0.000000001
            "L" to "m³" -> 0.001
            "mL" to "m³" -> 0.000001
            "gal" to "m³" -> 0.00378541
            "qt" to "m³" -> 0.000946353
            "pt" to "m³" -> 0.000473176
            "cup" to "m³" -> 0.000236588
            "fl oz" to "m³" -> 2.95735e-5
            
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
        return "${"%.${digits}f".format(value)} $unit"
    }
    
    /**
     * Obtiene la fecha y hora legible de la medición.
     */
    fun getFormattedDate(): String {
        return java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}

enum class MeasurementType {
    DISTANCE, AREA, VOLUME, ANGLE, HEIGHT, WIDTH, DEPTH
}

/**
 * Motor de medición avanzado que utiliza algoritmos precisos para realizar mediciones
 * en 2D y 3D, con soporte para fusión de sensores y calibración.
 */
class MeasurementEngine {
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .build()
    )
    
    // Caché para resultados intermedios
    private val measurementCache = LruCache<MeasurementCacheKey, MeasurementResult>(MeasurementConstants.CACHE_SIZE)
    
    // Estado de calibración
    private var calibrationFactor = 1.0
    private var referenceObjectSize = 0.0
    private var isCalibrated = false
    private var calibrationTimestamp: Long = 0
    
    // Parámetros de la cámara
    private var focalLength = 28.0 // mm, valor típico de smartphone
    private var sensorWidth = 4.8 // mm, ancho del sensor típico
    private var imageWidth = 1920 // píxeles, se actualiza con la cámara
    
    // Historial de mediciones para filtrado
    private val measurementHistory = LinkedHashMap<MeasurementType, MutableList<MeasurementResult>>()
    
    // Bloqueo para operaciones concurrentes
    private val lock = java.util.concurrent.locks.ReentrantLock()
    
    // Clave para el caché de mediciones
    private data class MeasurementCacheKey(
        val type: MeasurementType,
        val pointsHash: Int,
        val method: String
    )

    // ========== Calibración ==========
    
    /**
     * Calibra el sistema utilizando un objeto de referencia de tamaño conocido.
     * @param knownSize Tamaño real del objeto de referencia en metros
     * @param measuredPixels Tamaño medido en píxeles
     * @throws MeasurementError.InvalidCalibrationDistance Si el tamaño está fuera de rango
     */
    fun calibrateWithReferenceObject(knownSize: Double, measuredPixels: Double) {
        require(knownSize > 0) { "El tamaño conocido debe ser mayor que cero" }
        require(measuredPixels > 0) { "El tamaño medido en píxeles debe ser mayor que cero" }
        
        if (knownSize < MeasurementConstants.MIN_CALIBRATION_DISTANCE) {
            throw MeasurementError.InvalidCalibrationDistance(
                knownSize,
                MeasurementConstants.MIN_CALIBRATION_DISTANCE,
                MeasurementConstants.MAX_CALIBRATION_DISTANCE
            )
        }
        
        if (knownSize > MeasurementConstants.MAX_CALIBRATION_DISTANCE) {
            throw MeasurementError.InvalidCalibrationDistance(
                knownSize,
                MeasurementConstants.MIN_CALIBRATION_DISTANCE,
                MeasurementConstants.MAX_CALIBRATION_DISTANCE
            )
        }
        
        lock.lock()
        try {
            calibrationFactor = knownSize / measuredPixels
            referenceObjectSize = knownSize
            isCalibrated = true
            calibrationTimestamp = System.currentTimeMillis()
            
            // Limpiar caché después de la calibración
            measurementCache.clear()
        } finally {
            lock.unlock()
        }
    }
    
    /**
     * Obtiene el factor de calibración actual.
     */
    fun getCalibrationFactor(): Double = lock.withLock { calibrationFactor }
    
    /**
     * Verifica si el sistema está calibrado.
     */
    fun isCalibrated(): Boolean = lock.withLock { isCalibrated }

    // ========== Mediciones ==========
    
    /**
     * Mide la distancia entre dos puntos con alta precisión.
     * @param point1 Primer punto de medición
     * @param point2 Segundo punto de medición
     * @param use3D Si es true, utiliza datos de profundidad si están disponibles
     * @return Resultado de la medición de distancia
     */
    fun measureDistance(
        point1: MeasurementPoint,
        point2: MeasurementPoint,
        use3D: Boolean = false
    ): MeasurementResult {
        // Validar puntos
        point1.validate()
        point2.validate()
        
        // Verificar si el sistema está calibrado
        if (!isCalibrated) {
            throw MeasurementError.NotCalibrated()
        }
        
        // Crear clave de caché
        val cacheKey = MeasurementCacheKey(
            type = MeasurementType.DISTANCE,
            pointsHash = Objects.hash(point1, point2, use3D),
            method = if (use3D) "3D Distance" else "2D Distance"
        )
        
        // Usar caché si está disponible
        return measurementCache.getOrCompute(cacheKey) {
            // Calcular distancia
            val distance = if (use3D && hasDepthData(point1, point2)) {
                calculate3DDistance(point1, point2)
            } else {
                calculateEuclideanDistance(point1, point2) * calibrationFactor
            }
            
            // Calcular confianza
            val confidence = calculateConfidence(point1, point2)
            
            // Crear y retornar resultado
            MeasurementResult(
                type = MeasurementType.DISTANCE,
                value = distance,
                unit = "m",
                confidence = confidence,
                points = listOf(point1, point2),
                method = if (use3D) "3D Distance" else "2D Distance",
                metadata = mapOf(
                    "is3D" to use3D,
                    "calibrationFactor" to calibrationFactor,
                    "timestamp" to System.currentTimeMillis()
                )
            ).also { addToHistory(it) }
        }
    }

    /**
     * Calcula el área de un polígono definido por una lista de puntos.
     * @param points Lista de puntos que definen el polígono (mínimo 3)
     * @return Resultado de la medición de área
     */
    fun measureArea(points: List<MeasurementPoint>): MeasurementResult {
        // Validar puntos
        if (points.size < 3) {
            throw MeasurementError.InsufficientPoints(3, points.size)
        }
        points.forEach { it.validate() }
        
        // Verificar si el sistema está calibrado
        if (!isCalibrated) {
            throw MeasurementError.NotCalibrated()
        }
        
        // Crear clave de caché
        val cacheKey = MeasurementCacheKey(
            type = MeasurementType.AREA,
            pointsHash = points.hashCode(),
            method = "Polygon Area"
        )
        
        // Usar caché si está disponible
        return measurementCache.getOrCompute(cacheKey) {
            // Calcular área usando la fórmula del área de Gauss
            val area = calculatePolygonArea(points) * calibrationFactor * calibrationFactor
            
            // Calcular confianza promedio
            val confidence = calculateAverageConfidence(points)
            
            // Crear y retornar resultado
            MeasurementResult(
                type = MeasurementType.AREA,
                value = area,
                unit = "m²",
                confidence = confidence,
                points = points,
                method = "Polygon Area",
                metadata = mapOf(
                    "pointCount" to points.size,
                    "calibrationFactor" to calibrationFactor,
                    "timestamp" to System.currentTimeMillis()
                )
            ).also { addToHistory(it) }
        }
    }

    /**
     * Calcula el volumen de un paralelepípedo rectangular.
     * @param length Longitud en metros
     * @param width Ancho en metros
     * @param height Altura en metros
     * @return Resultado de la medición de volumen
     */
    fun measureVolume(length: Double, width: Double, height: Double): MeasurementResult {
        // Validar parámetros
        require(length > 0) { "La longitud debe ser mayor que cero" }
        require(width > 0) { "El ancho debe ser mayor que cero" }
        require(height > 0) { "La altura debe ser mayor que cero" }
        
        // Crear clave de caché
        val cacheKey = MeasurementCacheKey(
            type = MeasurementType.VOLUME,
            pointsHash = Objects.hash(length, width, height),
            method = "Cubic Volume"
        )
        
        // Usar caché si está disponible
        return measurementCache.getOrCompute(cacheKey) {
            // Calcular volumen
            val volume = length * width * height
            
            // Alta confianza para cálculos directos
            val confidence = 0.98
            
            // Crear y retornar resultado
            MeasurementResult(
                type = MeasurementType.VOLUME,
                value = volume,
                unit = "m³",
                confidence = confidence,
                points = emptyList(),
                method = "Cubic Volume",
                metadata = mapOf(
                    "dimensions" to mapOf("length" to length, "width" to width, "height" to height),
                    "timestamp" to System.currentTimeMillis()
                )
            ).also { addToHistory(it) }
        }
    }

    /**
     * Mide el ángulo formado por tres puntos (vértice en el segundo parámetro).
     * @param point1 Primer punto
    
    // Crear clave de caché
    val cacheKey = MeasurementCacheKey(
        type = MeasurementType.DISTANCE,
        pointsHash = Objects.hash(point1, point2, use3D),
        method = if (use3D) "3D Distance" else "2D Distance"
    )
    
    // Usar caché si está disponible
    return measurementCache.getOrCompute(cacheKey) {
        // Calcular distancia
        val distance = if (use3D && hasDepthData(point1, point2)) {
            calculate3DDistance(point1, point2)
        } else {
            calculateEuclideanDistance(point1, point2) * calibrationFactor
        }
        
        // Calcular confianza
        val confidence = calculateConfidence(point1, point2)
        
        // Crear y retornar resultado
        MeasurementResult(
            type = MeasurementType.DISTANCE,
            value = distance,
            unit = "m",
            confidence = confidence,
            points = listOf(point1, point2),
            method = if (use3D) "3D Distance" else "2D Distance",
            metadata = mapOf(
                "is3D" to use3D,
                "calibrationFactor" to calibrationFactor,
                "timestamp" to System.currentTimeMillis()
            )
        ).also { addToHistory(it) }
    }
    private fun getKnownObjectSize(labels: List<String>): Float {
        return when {
            labels.any { it.contains("coin") } -> 0.025f // 25mm diámetro
            labels.any { it.contains("card") } -> 0.085f // 85mm ancho
            labels.any { it.contains("phone") } -> 0.075f // 75mm ancho
            else -> 0f
        }
    }

    // Variables de calibración
    private var focalLength = 28f // mm, valor típico de smartphone

    fun setFocalLength(focal: Float) {
        focalLength = focal
    }
}

data class DetectedObject(
    val boundingBox: android.graphics.Rect,
    val confidence: Float,
    val trackingId: Int?,
    val labels: List<String>
) 