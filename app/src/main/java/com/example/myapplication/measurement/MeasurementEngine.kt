package com.example.myapplication.measurement

import android.content.Context
import android.graphics.*
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.myapplication.sensors.SensorData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

data class MeasurementPoint(
    val x: Float,
    val y: Float,
    val worldX: Float = 0f,
    val worldY: Float = 0f,
    val worldZ: Float = 0f,
    val confidence: Float = 1f
)

data class MeasurementResult(
    val type: MeasurementType,
    val value: Float,
    val unit: String,
    val confidence: Float,
    val points: List<MeasurementPoint>,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MeasurementType {
    LENGTH, WIDTH, HEIGHT, DISTANCE, AREA, VOLUME, ANGLE, DIAMETER, CIRCUMFERENCE
}

data class DetectedObject(
    val boundingBox: RectF,
    val confidence: Float,
    val label: String,
    val estimatedRealSize: Float? = null
)

class MeasurementEngine(private val context: Context) {
    
    private val _measurementResults = MutableStateFlow<List<MeasurementResult>>(emptyList())
    val measurementResults: StateFlow<List<MeasurementResult>> = _measurementResults.asStateFlow()
    
    private val _detectedObjects = MutableStateFlow<List<DetectedObject>>(emptyList())
    val detectedObjects: StateFlow<List<DetectedObject>> = _detectedObjects.asStateFlow()
    
    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated: StateFlow<Boolean> = _isCalibrated.asStateFlow()
    
    // ML Kit Object Detector
    private val objectDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
    
    private val objectDetector: ObjectDetector = ObjectDetection.getClient(objectDetectorOptions)
    
    // Variables de calibración
    private var pixelsPerMeter: Float = 1000f // valor por defecto
    private var cameraHeight: Float = 1.5f // altura típica del teléfono en metros
    private var cameraTiltAngle: Float = 0f
    private var focalLengthMm: Float = 4.25f // típico para smartphones
    private var sensorSizeMm: Float = 5.76f // altura del sensor típica
    
    // Puntos de medición seleccionados
    private val measurementPoints = mutableListOf<MeasurementPoint>()
    
    // Base de datos de objetos conocidos para calibración automática
    private val knownObjects = mapOf(
        "tarjeta de crédito" to 85.6f, // mm de ancho
        "moneda" to 24.26f, // mm diámetro moneda común
        "smartphone" to 150f, // mm altura promedio
        "libro" to 200f, // mm altura promedio
        "botella" to 240f, // mm altura típica
        "lata" to 123f, // mm altura lata estándar
        "papel A4" to 297f, // mm altura
        "cd/dvd" to 120f, // mm diámetro
        "lápiz" to 175f, // mm longitud
        "mano humana" to 190f // mm longitud promedio
    )
    
    fun processImage(imageProxy: ImageProxy, sensorData: SensorData) {
        val image = imageProxy.image
        if (image != null) {
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
            
            // Actualizar datos del sensor
            updateSensorBasedCalibration(sensorData)
            
            // Detectar objetos para calibración automática
            detectObjectsForCalibration(inputImage)
            
            // Procesar mediciones si hay puntos seleccionados
            if (measurementPoints.size >= 2) {
                processMeasurement(imageProxy, sensorData)
            }
        }
        imageProxy.close()
    }
    
    private fun updateSensorBasedCalibration(sensorData: SensorData) {
        // Actualizar ángulo de inclinación de la cámara
        cameraTiltAngle = sensorData.orientation[1] // pitch
        
        // Ajustar altura efectiva basada en inclinación
        val effectiveHeight = cameraHeight * cos(Math.toRadians(cameraTiltAngle.toDouble())).toFloat()
        
        // Calcular píxeles por metro usando trigonometría
        updatePixelsPerMeter(effectiveHeight, cameraTiltAngle)
    }
    
    private fun updatePixelsPerMeter(height: Float, tiltAngle: Float) {
        // Calcular el campo de visión basado en la altura y ángulo
        val fieldOfViewRadians = 2 * atan(sensorSizeMm / (2 * focalLengthMm))
        val groundDistanceVisible = 2 * height * tan(fieldOfViewRadians / 2)
        
        // Asumir resolución típica de 1920x1080
        val imageHeightPixels = 1080f
        pixelsPerMeter = imageHeightPixels / groundDistanceVisible
        
        Log.d("MeasurementEngine", "Píxeles por metro actualizados: $pixelsPerMeter")
    }
    
    private fun detectObjectsForCalibration(inputImage: InputImage) {
        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val objects = detectedObjects.map { visionObject ->
                    val label = visionObject.labels.firstOrNull()?.text?.lowercase() ?: "desconocido"
                    val confidence = visionObject.labels.firstOrNull()?.confidence ?: 0f
                    val realSize = findKnownObjectSize(label)
                    
                    DetectedObject(
                        boundingBox = visionObject.boundingBox,
                        confidence = confidence,
                        label = label,
                        estimatedRealSize = realSize
                    )
                }
                
                _detectedObjects.value = objects
                
                // Intentar calibración automática con objetos conocidos
                attemptAutoCalibration(objects)
            }
            .addOnFailureListener { e ->
                Log.e("MeasurementEngine", "Error en detección de objetos", e)
            }
    }
    
    private fun findKnownObjectSize(label: String): Float? {
        return knownObjects.entries.find { (key, _) ->
            label.contains(key, ignoreCase = true)
        }?.value
    }
    
    private fun attemptAutoCalibration(objects: List<DetectedObject>) {
        val knownObject = objects.find { it.estimatedRealSize != null && it.confidence > 0.7f }
        
        knownObject?.let { obj ->
            val pixelSize = obj.boundingBox.width() // usar ancho como referencia
            val realSizeMm = obj.estimatedRealSize!!
            val realSizeM = realSizeMm / 1000f
            
            pixelsPerMeter = pixelSize / realSizeM
            _isCalibrated.value = true
            
            Log.d("MeasurementEngine", "Calibración automática exitosa con ${obj.label}: $pixelsPerMeter px/m")
        }
    }
    
    fun addMeasurementPoint(x: Float, y: Float, imageWidth: Int, imageHeight: Int) {
        // Convertir coordenadas de pantalla a coordenadas del mundo real
        val worldCoords = screenToWorldCoordinates(x, y, imageWidth, imageHeight)
        
        val point = MeasurementPoint(
            x = x,
            y = y,
            worldX = worldCoords.first,
            worldY = worldCoords.second,
            worldZ = 0f // asumimos plano horizontal por ahora
        )
        
        measurementPoints.add(point)
        Log.d("MeasurementEngine", "Punto agregado: (${point.worldX}, ${point.worldY})")
    }
    
    private fun screenToWorldCoordinates(screenX: Float, screenY: Float, imageWidth: Int, imageHeight: Int): Pair<Float, Float> {
        // Convertir coordenadas de pantalla a metros reales
        val centerX = imageWidth / 2f
        val centerY = imageHeight / 2f
        
        val relativeX = (screenX - centerX) / pixelsPerMeter
        val relativeY = (screenY - centerY) / pixelsPerMeter
        
        // Ajustar por perspectiva y inclinación de la cámara
        val correctedY = relativeY / cos(Math.toRadians(cameraTiltAngle.toDouble())).toFloat()
        
        return Pair(relativeX, correctedY)
    }
    
    private fun processMeasurement(imageProxy: ImageProxy, sensorData: SensorData) {
        when {
            measurementPoints.size == 2 -> {
                // Medición de distancia/longitud
                val result = measureDistance(measurementPoints[0], measurementPoints[1])
                addMeasurementResult(result)
            }
            measurementPoints.size == 3 -> {
                // Medición de ángulo
                val result = measureAngle(measurementPoints[0], measurementPoints[1], measurementPoints[2])
                addMeasurementResult(result)
            }
            measurementPoints.size >= 4 -> {
                // Medición de área
                val result = measureArea(measurementPoints)
                addMeasurementResult(result)
            }
        }
    }
    
    private fun measureDistance(point1: MeasurementPoint, point2: MeasurementPoint): MeasurementResult {
        val deltaX = point2.worldX - point1.worldX
        val deltaY = point2.worldY - point1.worldY
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        
        // Determinar unidad apropiada
        val (value, unit) = when {
            distance < 0.01f -> Pair(distance * 1000, "mm")
            distance < 1f -> Pair(distance * 100, "cm")
            else -> Pair(distance, "m")
        }
        
        return MeasurementResult(
            type = MeasurementType.LENGTH,
            value = value,
            unit = unit,
            confidence = calculateMeasurementConfidence(),
            points = listOf(point1, point2)
        )
    }
    
    private fun measureAngle(point1: MeasurementPoint, vertex: MeasurementPoint, point2: MeasurementPoint): MeasurementResult {
        val vector1X = point1.worldX - vertex.worldX
        val vector1Y = point1.worldY - vertex.worldY
        val vector2X = point2.worldX - vertex.worldX
        val vector2Y = point2.worldY - vertex.worldY
        
        val dot = vector1X * vector2X + vector1Y * vector2Y
        val mag1 = sqrt(vector1X * vector1X + vector1Y * vector1Y)
        val mag2 = sqrt(vector2X * vector2X + vector2Y * vector2Y)
        
        val angleRadians = acos(dot / (mag1 * mag2))
        val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()
        
        return MeasurementResult(
            type = MeasurementType.ANGLE,
            value = angleDegrees,
            unit = "°",
            confidence = calculateMeasurementConfidence(),
            points = listOf(point1, vertex, point2)
        )
    }
    
    private fun measureArea(points: List<MeasurementPoint>): MeasurementResult {
        // Usar fórmula del shoelace para calcular área del polígono
        var area = 0f
        val n = points.size
        
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].worldX * points[j].worldY
            area -= points[j].worldX * points[i].worldY
        }
        
        area = abs(area) / 2f
        
        // Determinar unidad apropiada
        val (value, unit) = when {
            area < 0.0001f -> Pair(area * 1_000_000, "mm²")
            area < 1f -> Pair(area * 10_000, "cm²")
            else -> Pair(area, "m²")
        }
        
        return MeasurementResult(
            type = MeasurementType.AREA,
            value = value,
            unit = unit,
            confidence = calculateMeasurementConfidence(),
            points = points
        )
    }
    
    private fun calculateMeasurementConfidence(): Float {
        var confidence = 1f
        
        // Reducir confianza si no estamos calibrados
        if (!_isCalibrated.value) {
            confidence *= 0.5f
        }
        
        // Reducir confianza basado en inclinación de la cámara
        val tiltPenalty = abs(cameraTiltAngle) / 45f // penalidad máxima a 45 grados
        confidence *= (1f - tiltPenalty * 0.3f)
        
        // Reducir confianza si hay pocos puntos de referencia
        if (measurementPoints.size < 3) {
            confidence *= 0.8f
        }
        
        return max(0.1f, confidence)
    }
    
    private fun addMeasurementResult(result: MeasurementResult) {
        val currentResults = _measurementResults.value.toMutableList()
        currentResults.add(result)
        _measurementResults.value = currentResults
        
        Log.d("MeasurementEngine", "Nueva medición: ${result.value} ${result.unit} (confianza: ${result.confidence})")
    }
    
    fun clearMeasurementPoints() {
        measurementPoints.clear()
    }
    
    fun undoLastPoint() {
        if (measurementPoints.isNotEmpty()) {
            measurementPoints.removeAt(measurementPoints.size - 1)
        }
    }
    
    fun manualCalibration(knownDistanceMm: Float, pixelDistance: Float) {
        pixelsPerMeter = pixelDistance / (knownDistanceMm / 1000f)
        _isCalibrated.value = true
        Log.d("MeasurementEngine", "Calibración manual: $pixelsPerMeter px/m")
    }
    
    fun setCameraParameters(focalLength: Float, sensorSize: Float, height: Float) {
        focalLengthMm = focalLength
        sensorSizeMm = sensorSize
        cameraHeight = height
    }
    
    fun getCurrentMeasurementPoints(): List<MeasurementPoint> {
        return measurementPoints.toList()
    }
    
    fun exportMeasurements(): String {
        val results = _measurementResults.value
        val builder = StringBuilder()
        
        builder.append("Reporte de Mediciones\n")
        builder.append("=====================\n")
        builder.append("Fecha: ${java.util.Date()}\n")
        builder.append("Calibración: ${if (_isCalibrated.value) "Sí" else "No"}\n")
        builder.append("Píxeles por metro: $pixelsPerMeter\n\n")
        
        results.forEachIndexed { index, result ->
            builder.append("Medición ${index + 1}:\n")
            builder.append("  Tipo: ${result.type}\n")
            builder.append("  Valor: ${result.value} ${result.unit}\n")
            builder.append("  Confianza: ${(result.confidence * 100).toInt()}%\n")
            builder.append("  Puntos: ${result.points.size}\n\n")
        }
        
        return builder.toString()
    }
    
    fun cleanup() {
        objectDetector.close()
        measurementPoints.clear()
        _measurementResults.value = emptyList()
        _detectedObjects.value = emptyList()
    }
}
