package com.example.myapplication.ar

import android.content.Context
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.UnavailableException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

data class AR3DPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val confidence: Float = 1f,
    val anchorId: String? = null
)

data class AR3DMeasurement(
    val startPoint: AR3DPoint,
    val endPoint: AR3DPoint,
    val distance: Float,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class ARPlane(
    val center: AR3DPoint,
    val normal: FloatArray,
    val extent: FloatArray,
    val type: Plane.Type
)

class ARMeasurementEngine(private val context: Context) {
    
    private var arSession: Session? = null
    private val anchors = mutableListOf<Anchor>()
    private val measurementPoints = mutableListOf<AR3DPoint>()
    
    private val _arMeasurements = MutableStateFlow<List<AR3DMeasurement>>(emptyList())
    val arMeasurements: StateFlow<List<AR3DMeasurement>> = _arMeasurements.asStateFlow()
    
    private val _detectedPlanes = MutableStateFlow<List<ARPlane>>(emptyList())
    val detectedPlanes: StateFlow<List<ARPlane>> = _detectedPlanes.asStateFlow()
    
    private val _isARAvailable = MutableStateFlow(false)
    val isARAvailable: StateFlow<Boolean> = _isARAvailable.asStateFlow()
    
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    // Matrices para cálculos 3D
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    
    suspend fun initializeAR(): Boolean {
        return try {
            // Verificar disponibilidad de ARCore
            when (ArCoreApk.getInstance().checkAvailability(context)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    createARSession()
                    true
                }
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    Log.w("ARMeasurement", "ARCore necesita actualización o instalación")
                    false
                }
                else -> {
                    Log.e("ARMeasurement", "ARCore no es compatible con este dispositivo")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("ARMeasurement", "Error inicializando AR", e)
            false
        }
    }
    
    private fun createARSession() {
        arSession = Session(context).apply {
            // Configurar sesión AR
            val config = Config(this).apply {
                // Habilitar detección de planos horizontales y verticales
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                
                // Habilitar oclusión ambiental si está disponible
                if (isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    depthMode = Config.DepthMode.AUTOMATIC
                }
                
                // Configurar modo de seguimiento de luz
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                
                // Habilitar detección de objetos instantáneos
                instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            }
            
            configure(config)
            _isARAvailable.value = true
            
            Log.d("ARMeasurement", "ARCore inicializado exitosamente")
        }
    }
    
    fun updateARFrame(frame: Frame) {
        arSession?.let { session ->
            try {
                // Actualizar estado de seguimiento
                val camera = frame.camera
                _trackingState.value = camera.trackingState
                
                if (camera.trackingState == TrackingState.TRACKING) {
                    // Actualizar matrices de vista y proyección
                    camera.getViewMatrix(viewMatrix, 0)
                    camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
                    
                    // Detectar y actualizar planos
                    updateDetectedPlanes(frame)
                    
                    // Procesar puntos de medición si existen
                    if (measurementPoints.size >= 2) {
                        processAR3DMeasurement()
                    }
                }
            } catch (e: Exception) {
                Log.e("ARMeasurement", "Error procesando frame AR", e)
            }
        }
    }
    
    private fun updateDetectedPlanes(frame: Frame) {
        val planes = frame.getUpdatedTrackables(Plane::class.java)
        val arPlanes = planes.mapNotNull { plane ->
            if (plane.trackingState == TrackingState.TRACKING) {
                val pose = plane.centerPose
                val center = AR3DPoint(
                    x = pose.tx(),
                    y = pose.ty(),
                    z = pose.tz()
                )
                
                val normal = FloatArray(3)
                pose.getRotationQuaternion(normal, 0)
                
                ARPlane(
                    center = center,
                    normal = normal,
                    extent = floatArrayOf(plane.extentX, plane.extentZ),
                    type = plane.type
                )
            } else null
        }
        
        _detectedPlanes.value = arPlanes
    }
    
    fun addAR3DMeasurementPoint(screenX: Float, screenY: Float, frame: Frame): Boolean {
        arSession?.let { session ->
            try {
                val hits = frame.hitTest(screenX, screenY)
                
                // Buscar el mejor hit en un plano detectado
                val bestHit = hits.firstOrNull { hit ->
                    val trackable = hit.trackable
                    trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                } ?: hits.firstOrNull() // fallback al primer hit disponible
                
                bestHit?.let { hit ->
                    // Crear anchor para este punto
                    val anchor = hit.createAnchor()
                    anchors.add(anchor)
                    
                    val pose = hit.hitPose
                    val point = AR3DPoint(
                        x = pose.tx(),
                        y = pose.ty(),
                        z = pose.tz(),
                        confidence = calculateHitConfidence(hit),
                        anchorId = anchor.toString()
                    )
                    
                    measurementPoints.add(point)
                    
                    Log.d("ARMeasurement", "Punto 3D agregado: (${point.x}, ${point.y}, ${point.z})")
                    return true
                }
            } catch (e: Exception) {
                Log.e("ARMeasurement", "Error agregando punto 3D", e)
            }
        }
        return false
    }
    
    private fun calculateHitConfidence(hit: HitResult): Float {
        val distance = hit.distance
        val trackable = hit.trackable
        
        var confidence = 1f
        
        // Reducir confianza basado en distancia
        confidence *= when {
            distance < 0.5f -> 0.9f
            distance < 1f -> 1f
            distance < 3f -> 0.8f
            distance < 5f -> 0.6f
            else -> 0.4f
        }
        
        // Aumentar confianza si hit está en un plano detectado
        if (trackable is Plane) {
            confidence *= 1.2f
        }
        
        return minOf(confidence, 1f)
    }
    
    private fun processAR3DMeasurement() {
        if (measurementPoints.size >= 2) {
            val startPoint = measurementPoints[measurementPoints.size - 2]
            val endPoint = measurementPoints[measurementPoints.size - 1]
            
            val distance = calculate3DDistance(startPoint, endPoint)
            val confidence = minOf(startPoint.confidence, endPoint.confidence)
            
            val measurement = AR3DMeasurement(
                startPoint = startPoint,
                endPoint = endPoint,
                distance = distance,
                confidence = confidence
            )
            
            val currentMeasurements = _arMeasurements.value.toMutableList()
            currentMeasurements.add(measurement)
            _arMeasurements.value = currentMeasurements
            
            Log.d("ARMeasurement", "Nueva medición 3D: ${distance}m (confianza: ${confidence})")
        }
    }
    
    private fun calculate3DDistance(point1: AR3DPoint, point2: AR3DPoint): Float {
        val dx = point2.x - point1.x
        val dy = point2.y - point1.y
        val dz = point2.z - point1.z
        
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    fun measureVolume(points: List<AR3DPoint>): Float {
        if (points.size < 4) return 0f
        
        // Usar algoritmo de triangulación para calcular volumen
        // Simplificado: asumir forma aproximadamente rectangular
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val minZ = points.minOf { it.z }
        val maxZ = points.maxOf { it.z }
        
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ)
    }
    
    fun measureAreaOnPlane(points: List<AR3DPoint>, plane: ARPlane): Float {
        if (points.size < 3) return 0f
        
        // Proyectar puntos al plano y calcular área usando shoelace formula
        var area = 0f
        val n = points.size
        
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].x * points[j].z - points[j].x * points[i].z
        }
        
        return kotlin.math.abs(area) / 2f
    }
    
    fun getDistanceToPlane(point: AR3DPoint, plane: ARPlane): Float {
        // Calcular distancia punto-plano usando fórmula estándar
        val normal = plane.normal
        val planeCenter = plane.center
        
        val dx = point.x - planeCenter.x
        val dy = point.y - planeCenter.y
        val dz = point.z - planeCenter.z
        
        return kotlin.math.abs(dx * normal[0] + dy * normal[1] + dz * normal[2])
    }
    
    fun projectPointToScreen(point: AR3DPoint, viewMatrix: FloatArray, projectionMatrix: FloatArray, viewportWidth: Int, viewportHeight: Int): Pair<Float, Float>? {
        val worldPosition = floatArrayOf(point.x, point.y, point.z, 1f)
        val viewPosition = FloatArray(4)
        val clipPosition = FloatArray(4)
        
        // Transformar a espacio de vista
        Matrix.multiplyMV(viewPosition, 0, viewMatrix, 0, worldPosition, 0)
        
        // Transformar a espacio de clip
        Matrix.multiplyMV(clipPosition, 0, projectionMatrix, 0, viewPosition, 0)
        
        // Verificar si está detrás de la cámara
        if (clipPosition[3] <= 0) return null
        
        // Normalizar coordenadas del dispositivo (NDC)
        val ndcX = clipPosition[0] / clipPosition[3]
        val ndcY = clipPosition[1] / clipPosition[3]
        
        // Convertir a coordenadas de pantalla
        val screenX = (ndcX + 1f) * 0.5f * viewportWidth
        val screenY = (1f - ndcY) * 0.5f * viewportHeight
        
        return Pair(screenX, screenY)
    }
    
    fun clearMeasurementPoints() {
        measurementPoints.clear()
        
        // Limpiar anchors
        anchors.forEach { anchor ->
            anchor.detach()
        }
        anchors.clear()
    }
    
    fun undoLastPoint() {
        if (measurementPoints.isNotEmpty()) {
            measurementPoints.removeAt(measurementPoints.size - 1)
            
            if (anchors.isNotEmpty()) {
                anchors.removeAt(anchors.size - 1).detach()
            }
        }
    }
    
    fun getCurrentMeasurementPoints(): List<AR3DPoint> {
        return measurementPoints.toList()
    }
    
    fun pauseARSession() {
        arSession?.pause()
    }
    
    fun resumeARSession() {
        try {
            arSession?.resume()
        } catch (e: Exception) {
            Log.e("ARMeasurement", "Error resumiendo sesión AR", e)
        }
    }
    
    fun cleanup() {
        anchors.forEach { it.detach() }
        anchors.clear()
        measurementPoints.clear()
        
        arSession?.close()
        arSession = null
        
        _isARAvailable.value = false
    }
    
    fun exportAR3DMeasurements(): String {
        val measurements = _arMeasurements.value
        val planes = _detectedPlanes.value
        val points = measurementPoints
        
        val builder = StringBuilder()
        builder.append("Reporte de Mediciones AR 3D\n")
        builder.append("===========================\n")
        builder.append("Fecha: ${java.util.Date()}\n")
        builder.append("Estado de tracking: ${_trackingState.value}\n")
        builder.append("Planos detectados: ${planes.size}\n")
        builder.append("Puntos de medición: ${points.size}\n\n")
        
        // Información de planos
        builder.append("PLANOS DETECTADOS:\n")
        planes.forEachIndexed { index, plane ->
            builder.append("Plano ${index + 1}:\n")
            builder.append("  Tipo: ${plane.type}\n")
            builder.append("  Centro: (${plane.center.x}, ${plane.center.y}, ${plane.center.z})\n")
            builder.append("  Extensión: ${plane.extent[0]} x ${plane.extent[1]}\n\n")
        }
        
        // Mediciones 3D
        builder.append("MEDICIONES 3D:\n")
        measurements.forEachIndexed { index, measurement ->
            builder.append("Medición ${index + 1}:\n")
            builder.append("  Distancia: ${String.format("%.3f", measurement.distance)}m\n")
            builder.append("  Confianza: ${(measurement.confidence * 100).toInt()}%\n")
            builder.append("  Inicio: (${measurement.startPoint.x}, ${measurement.startPoint.y}, ${measurement.startPoint.z})\n")
            builder.append("  Final: (${measurement.endPoint.x}, ${measurement.endPoint.y}, ${measurement.endPoint.z})\n\n")
        }
        
        return builder.toString()
    }
}
