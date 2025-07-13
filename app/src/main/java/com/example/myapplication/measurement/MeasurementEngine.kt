package com.example.myapplication.measurement

import android.graphics.Bitmap
import android.graphics.PointF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.math.*

data class MeasurementPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class MeasurementResult(
    val type: MeasurementType,
    val value: Float,
    val unit: String,
    val confidence: Float,
    val points: List<MeasurementPoint>,
    val method: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MeasurementType {
    DISTANCE, AREA, VOLUME, ANGLE, HEIGHT, WIDTH, DEPTH
}

class MeasurementEngine {

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .build()
    )

    companion object {
        const val EARTH_RADIUS = 6371000f // metros
        const val GRAVITY = 9.80665f // m/s²
        const val PI = 3.14159265359f
    }

    // Calibración del sistema
    private var calibrationFactor = 1.0f
    private var referenceObjectSize = 0f
    private var isCalibrated = false

    fun calibrateWithReferenceObject(knownSize: Float, measuredPixels: Float) {
        calibrationFactor = knownSize / measuredPixels
        referenceObjectSize = knownSize
        isCalibrated = true
    }

    fun measureDistance(point1: MeasurementPoint, point2: MeasurementPoint): MeasurementResult {
        val distance = calculateEuclideanDistance(point1, point2)
        val confidence = calculateConfidence(point1, point2)

        return MeasurementResult(
            type = MeasurementType.DISTANCE,
            value = distance * calibrationFactor,
            unit = "m",
            confidence = confidence,
            points = listOf(point1, point2),
            method = "Euclidean Distance"
        )
    }

    fun measureArea(points: List<MeasurementPoint>): MeasurementResult {
        if (points.size < 3) {
            throw IllegalArgumentException("Se necesitan al menos 3 puntos para calcular área")
        }

        val area = calculatePolygonArea(points)
        val confidence = calculateAverageConfidence(points)

        return MeasurementResult(
            type = MeasurementType.AREA,
            value = area * calibrationFactor * calibrationFactor,
            unit = "m²",
            confidence = confidence,
            points = points,
            method = "Polygon Area"
        )
    }

    fun measureVolume(length: Float, width: Float, height: Float): MeasurementResult {
        val volume = length * width * height
        val confidence = 0.9f // Alta confianza para cálculos directos

        return MeasurementResult(
            type = MeasurementType.VOLUME,
            value = volume,
            unit = "m³",
            confidence = confidence,
            points = emptyList(),
            method = "Cubic Volume"
        )
    }

    fun measureAngle(
        point1: MeasurementPoint,
        vertex: MeasurementPoint,
        point2: MeasurementPoint
    ): MeasurementResult {
        val angle = calculateAngle(point1, vertex, point2)
        val confidence = calculateConfidence(point1, point2)

        return MeasurementResult(
            type = MeasurementType.ANGLE,
            value = angle,
            unit = "°",
            confidence = confidence,
            points = listOf(point1, vertex, point2),
            method = "Vector Angle"
        )
    }

    fun detectObjectsInImage(bitmap: Bitmap, onObjectsDetected: (List<DetectedObject>) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                val objects = detectedObjects.map { obj ->
                    DetectedObject(
                        boundingBox = obj.boundingBox,
                        confidence = obj.confidence,
                        trackingId = obj.trackingId,
                        labels = obj.labels.map { it.text }
                    )
                }
                onObjectsDetected(objects)
            }
            .addOnFailureListener { e ->
                // Manejar error
            }
    }

    // Algoritmos matemáticos reales

    private fun calculateEuclideanDistance(
        point1: MeasurementPoint,
        point2: MeasurementPoint
    ): Float {
        val dx = point2.x - point1.x
        val dy = point2.y - point1.y
        val dz = point2.z - point1.z

        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun calculatePolygonArea(points: List<MeasurementPoint>): Float {
        // Fórmula del área de Gauss (Shoelace formula)
        var area = 0f
        val n = points.size

        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].x * points[j].y
            area -= points[j].x * points[i].y
        }

        return abs(area) / 2f
    }

    private fun calculateAngle(
        point1: MeasurementPoint,
        vertex: MeasurementPoint,
        point2: MeasurementPoint
    ): Float {
        // Calcular vectores
        val vector1 = PointF(point1.x - vertex.x, point1.y - vertex.y)
        val vector2 = PointF(point2.x - vertex.x, point2.y - vertex.y)

        // Calcular producto punto
        val dotProduct = vector1.x * vector2.x + vector1.y * vector2.y

        // Calcular magnitudes
        val magnitude1 = sqrt(vector1.x * vector1.x + vector1.y * vector1.y)
        val magnitude2 = sqrt(vector2.x * vector2.x + vector2.y * vector2.y)

        // Calcular ángulo
        val cosAngle = dotProduct / (magnitude1 * magnitude2)
        val angleRadians = acos(cosAngle.coerceIn(-1f, 1f))

        return angleRadians * 180f / PI
    }

    private fun calculateConfidence(point1: MeasurementPoint, point2: MeasurementPoint): Float {
        var confidence = 1.0f

        // Reducir confianza basado en la confianza de los puntos
        confidence *= (point1.confidence + point2.confidence) / 2f

        // Reducir confianza si los puntos están muy separados (posible error)
        val distance = calculateEuclideanDistance(point1, point2)
        if (distance > 1000f) { // Más de 1000 píxeles
            confidence *= 0.8f
        }

        return confidence.coerceIn(0f, 1f)
    }

    private fun calculateAverageConfidence(points: List<MeasurementPoint>): Float {
        return points.map { it.confidence }.average().toFloat()
    }

    // Algoritmos avanzados para mediciones 3D

    fun calculate3DDistance(
        point1: MeasurementPoint,
        point2: MeasurementPoint,
        depthMap: Array<Array<Float>>
    ): MeasurementResult {
        val depth1 = getDepthAtPoint(point1, depthMap)
        val depth2 = getDepthAtPoint(point2, depthMap)

        val x1 = point1.x * depth1 / focalLength
        val y1 = point1.y * depth1 / focalLength
        val z1 = depth1

        val x2 = point2.x * depth2 / focalLength
        val y2 = point2.y * depth2 / focalLength
        val z2 = depth2

        val distance = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1))

        return MeasurementResult(
            type = MeasurementType.DISTANCE,
            value = distance,
            unit = "m",
            confidence = 0.95f,
            points = listOf(point1, point2),
            method = "3D Distance"
        )
    }

    private fun getDepthAtPoint(point: MeasurementPoint, depthMap: Array<Array<Float>>): Float {
        val x = point.x.toInt().coerceIn(0, depthMap[0].size - 1)
        val y = point.y.toInt().coerceIn(0, depthMap.size - 1)
        return depthMap[y][x]
    }

    // Algoritmos de fusión de sensores

    fun fuseMeasurements(measurements: List<MeasurementResult>): MeasurementResult {
        if (measurements.isEmpty()) {
            throw IllegalArgumentException("No hay mediciones para fusionar")
        }

        if (measurements.size == 1) {
            return measurements.first()
        }

        // Calcular promedio ponderado por confianza
        var totalWeight = 0f
        var weightedSum = 0f

        measurements.forEach { measurement ->
            val weight = measurement.confidence
            totalWeight += weight
            weightedSum += measurement.value * weight
        }

        val fusedValue = weightedSum / totalWeight
        val fusedConfidence = measurements.map { it.confidence }.average().toFloat()

        return MeasurementResult(
            type = measurements.first().type,
            value = fusedValue,
            unit = measurements.first().unit,
            confidence = fusedConfidence,
            points = measurements.flatMap { it.points },
            method = "Sensor Fusion"
        )
    }

    // Algoritmos de calibración automática

    fun autoCalibrate(knownObjects: List<DetectedObject>): Boolean {
        if (knownObjects.isEmpty()) return false

        // Buscar objetos de referencia conocidos (monedas, tarjetas, etc.)
        val referenceObject = knownObjects.find { obj ->
            obj.labels.any { label ->
                label.contains("coin") || label.contains("card") || label.contains("phone")
            }
        }

        referenceObject?.let { obj ->
            val knownSize = getKnownObjectSize(obj.labels)
            if (knownSize > 0) {
                val measuredSize = obj.boundingBox.width().toFloat()
                calibrateWithReferenceObject(knownSize, measuredSize)
                return true
            }
        }

        return false
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