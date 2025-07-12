package com.example.myapplication.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlin.math.*

data class CameraInfo(
    val id: String,
    val type: CameraType,
    val characteristics: CameraCharacteristics,
    val isAvailable: Boolean = true
)

enum class CameraType {
    WIDE, ULTRA_WIDE, TELEPHOTO, FRONT, MACRO, DEPTH
}

data class CameraMeasurement(
    val distance: Float,
    val confidence: Float,
    val method: MeasurementMethod,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MeasurementMethod {
    STEREO_VISION, DEPTH_SENSOR, FOCAL_LENGTH, AR_CORE
}

class MultiCameraManager(private val context: Context) {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    
    private var availableCameras = mutableListOf<CameraInfo>()
    private var activeCamera: Camera? = null
    private var imageReader: ImageReader? = null
    
    // Parámetros de calibración
    private var focalLength = 0f
    private var sensorWidth = 0f
    private var sensorHeight = 0f
    private var baseline = 0f // Distancia entre cámaras para visión estéreo
    
    companion object {
        private const val TAG = "MultiCameraManager"
        const val TARGET_IMAGE_SIZE = 1920
        const val MIN_CONFIDENCE = 0.7f
    }
    
    fun initialize(lifecycleOwner: LifecycleOwner) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture?.addListener({
            cameraProvider = cameraProviderFuture?.get()
            setupCameras()
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun setupCameras() {
        availableCameras.clear()
        
        try {
            val cameraIds = cameraManager.cameraIdList
            
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val cameraType = determineCameraType(characteristics)
                
                val cameraInfo = CameraInfo(
                    id = cameraId,
                    type = cameraType,
                    characteristics = characteristics,
                    isAvailable = true
                )
                
                availableCameras.add(cameraInfo)
                Log.d(TAG, "Camera found: $cameraType (ID: $cameraId)")
            }
            
            // Configurar parámetros de calibración
            setupCalibrationParameters()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up cameras", e)
        }
    }
    
    private fun determineCameraType(characteristics: CameraCharacteristics): CameraType {
        val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        
        return when {
            facing == CameraCharacteristics.LENS_FACING_FRONT -> CameraType.FRONT
            capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) == true -> CameraType.DEPTH
            capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true -> CameraType.MACRO
            else -> {
                // Determinar tipo basado en focal length
                val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focalLength = focalLengths?.firstOrNull() ?: 0f
                
                when {
                    focalLength < 20f -> CameraType.ULTRA_WIDE
                    focalLength > 50f -> CameraType.TELEPHOTO
                    else -> CameraType.WIDE
                }
            }
        }
    }
    
    private fun setupCalibrationParameters() {
        // Obtener parámetros de la cámara principal
        val mainCamera = availableCameras.find { it.type == CameraType.WIDE }
        mainCamera?.let { camera ->
            val characteristics = camera.characteristics
            
            // Focal length
            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            focalLength = focalLengths?.firstOrNull() ?: 28f
            
            // Sensor size
            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            sensorWidth = sensorSize?.width?.toFloat() ?: 4032f
            sensorHeight = sensorSize?.height?.toFloat() ?: 3024f
            
            // Baseline para visión estéreo (si hay múltiples cámaras)
            if (availableCameras.size > 1) {
                baseline = calculateBaseline()
            }
            
            Log.d(TAG, "Calibration: focalLength=$focalLength, sensorSize=${sensorWidth}x${sensorHeight}, baseline=$baseline")
        }
    }
    
    private fun calculateBaseline(): Float {
        // Calcular distancia entre cámaras basado en características del dispositivo
        // Esto es una aproximación, en dispositivos reales se necesitaría calibración específica
        return when {
            availableCameras.size >= 3 -> 12f // mm
            availableCameras.size >= 2 -> 8f  // mm
            else -> 0f
        }
    }
    
    fun bindCamera(lifecycleOwner: LifecycleOwner, cameraType: CameraType): Camera? {
        val cameraInfo = availableCameras.find { it.type == cameraType }
        if (cameraInfo == null) {
            Log.w(TAG, "Camera type $cameraType not available")
            return null
        }
        
        return try {
            val camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.Builder().addCameraFilter { 
                    listOf(cameraInfo.id).map { CameraSelector.Builder().addCameraFilter { listOf() }.build() }
                }.build(),
                Preview.Builder().build(),
                ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
            )
            
            activeCamera = camera
            Log.d(TAG, "Camera $cameraType bound successfully")
            camera
            
        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera $cameraType", e)
            null
        }
    }
    
    fun measureDistance(objectWidth: Float, objectHeight: Float, imageWidth: Int, imageHeight: Int): CameraMeasurement {
        // Método 1: Usando focal length y tamaño conocido del objeto
        val distanceByFocalLength = calculateDistanceByFocalLength(objectWidth, imageWidth)
        
        // Método 2: Usando visión estéreo (si hay múltiples cámaras)
        val distanceByStereo = if (baseline > 0) {
            calculateDistanceByStereo()
        } else {
            null
        }
        
        // Método 3: Usando sensor de profundidad (si está disponible)
        val distanceByDepth = if (hasDepthCamera()) {
            calculateDistanceByDepth()
        } else {
            null
        }
        
        // Combinar resultados usando fusión de sensores
        val finalDistance = fuseDistanceMeasurements(
            distanceByFocalLength,
            distanceByStereo,
            distanceByDepth
        )
        
        val confidence = calculateConfidence(distanceByFocalLength, distanceByStereo, distanceByDepth)
        
        return CameraMeasurement(
            distance = finalDistance,
            confidence = confidence,
            method = determineBestMethod(distanceByStereo, distanceByDepth)
        )
    }
    
    private fun calculateDistanceByFocalLength(objectWidth: Float, imageWidth: Int): Float {
        // Fórmula: distance = (focal_length * object_width) / image_width
        return (focalLength * objectWidth) / imageWidth
    }
    
    private fun calculateDistanceByStereo(): Float? {
        // Implementación de visión estéreo
        // Requiere imágenes de dos cámaras simultáneas
        return null // Placeholder
    }
    
    private fun calculateDistanceByDepth(): Float? {
        // Implementación usando sensor de profundidad
        return null // Placeholder
    }
    
    private fun fuseDistanceMeasurements(
        focalLength: Float,
        stereo: Float?,
        depth: Float?
    ): Float {
        val measurements = mutableListOf<Float>()
        val weights = mutableListOf<Float>()
        
        // Agregar medición por focal length
        measurements.add(focalLength)
        weights.add(0.6f) // Peso alto para focal length
        
        // Agregar medición estéreo si está disponible
        stereo?.let {
            measurements.add(it)
            weights.add(0.3f)
        }
        
        // Agregar medición de profundidad si está disponible
        depth?.let {
            measurements.add(it)
            weights.add(0.1f)
        }
        
        // Calcular promedio ponderado
        val totalWeight = weights.sum()
        return measurements.zip(weights).sumOf { (measurement, weight) ->
            (measurement * weight / totalWeight).toDouble()
        }.toFloat()
    }
    
    private fun calculateConfidence(
        focalLength: Float,
        stereo: Float?,
        depth: Float?
    ): Float {
        var confidence = 0.7f // Base confidence
        
        // Aumentar confianza si tenemos múltiples métodos
        if (stereo != null) confidence += 0.2f
        if (depth != null) confidence += 0.1f
        
        // Verificar estabilidad de la medición
        if (abs(focalLength - (stereo ?: focalLength)) < 0.1f) {
            confidence += 0.1f
        }
        
        return minOf(confidence, 1.0f)
    }
    
    private fun determineBestMethod(stereo: Float?, depth: Float?): MeasurementMethod {
        return when {
            depth != null -> MeasurementMethod.DEPTH_SENSOR
            stereo != null -> MeasurementMethod.STEREO_VISION
            else -> MeasurementMethod.FOCAL_LENGTH
        }
    }
    
    private fun hasDepthCamera(): Boolean {
        return availableCameras.any { it.type == CameraType.DEPTH }
    }
    
    fun getAvailableCameras(): List<CameraInfo> = availableCameras.toList()
    
    fun release() {
        cameraProvider?.unbindAll()
        cameraThread.quitSafely()
    }
} 