package com.example.myapplication.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class CameraInfo(
    val id: String,
    val facing: Int,
    val type: CameraType,
    val hasFlash: Boolean,
    val hasAutoFocus: Boolean,
    val maxZoom: Float,
    val focalLength: Float,
    val sensorSize: android.util.SizeF,
    val supportedResolutions: List<android.util.Size>
)

enum class CameraType {
    WIDE, ULTRA_WIDE, TELEPHOTO, DEPTH, MACRO, FRONT, UNKNOWN
}

class MultiCameraManager(private val context: Context) {
    
    private val cameraExecutor: ExecutorService = Executors.newCachedThreadPool()
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraProvider: ProcessCameraProvider? = null
    
    private val _availableCameras = mutableListOf<CameraInfo>()
    val availableCameras: List<CameraInfo> get() = _availableCameras
    
    private var currentCamera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    
    suspend fun initializeCameras() {
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
            discoverAllCameras()
        } catch (e: Exception) {
            Log.e("MultiCameraManager", "Error inicializando cámaras", e)
        }
    }
    
    private fun discoverAllCameras() {
        _availableCameras.clear()
        
        try {
            val cameraIds = cameraManager.cameraIdList
            Log.d("MultiCameraManager", "Encontradas ${cameraIds.size} cámaras")
            
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val cameraInfo = analyzeCameraCharacteristics(cameraId, characteristics)
                _availableCameras.add(cameraInfo)
                
                Log.d("MultiCameraManager", "Cámara $cameraId: ${cameraInfo.type}")
            }
        } catch (e: Exception) {
            Log.e("MultiCameraManager", "Error descubriendo cámaras", e)
        }
    }
    
    private fun analyzeCameraCharacteristics(cameraId: String, characteristics: CameraCharacteristics): CameraInfo {
        val facing = characteristics.get(CameraCharacteristics.LENS_FACING) 
            ?: CameraCharacteristics.LENS_FACING_BACK
            
        val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val hasAutoFocus = minFocusDistance > 0f
        
        val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
        
        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val focalLength = focalLengths?.firstOrNull() ?: 0f
        
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) 
            ?: android.util.SizeF(0f, 0f)
            
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedSizes = configMap?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.toList() 
            ?: emptyList()
        
        val cameraType = determineCameraType(focalLength, facing, sensorSize, characteristics)
        
        return CameraInfo(
            id = cameraId,
            facing = facing,
            type = cameraType,
            hasFlash = flashAvailable,
            hasAutoFocus = hasAutoFocus,
            maxZoom = maxZoom,
            focalLength = focalLength,
            sensorSize = sensorSize,
            supportedResolutions = supportedSizes
        )
    }
    
    private fun determineCameraType(
        focalLength: Float, 
        facing: Int, 
        sensorSize: android.util.SizeF,
        characteristics: CameraCharacteristics
    ): CameraType {
        return when {
            facing == CameraCharacteristics.LENS_FACING_FRONT -> CameraType.FRONT
            focalLength < 20f -> CameraType.ULTRA_WIDE
            focalLength > 50f -> CameraType.TELEPHOTO
            focalLength in 20f..35f -> CameraType.WIDE
            else -> CameraType.UNKNOWN
        }
    }
    
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        surfaceProvider: Preview.SurfaceProvider,
        analyzer: ImageAnalysis.Analyzer? = null
    ): Camera? {
        return try {
            cameraProvider?.let { provider ->
                provider.unbindAll()
                
                // Configurar Preview
                preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                    .also {
                        it.setSurfaceProvider(surfaceProvider)
                    }
                
                // Configurar ImageCapture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                
                // Configurar ImageAnalysis si se proporciona analyzer
                imageAnalyzer = analyzer?.let {
                    ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor, it)
                        }
                }
                
                val useCases = mutableListOf<UseCase>(preview!!, imageCapture!!)
                imageAnalyzer?.let { useCases.add(it) }
                
                currentCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )
                
                currentCamera
            }
        } catch (e: Exception) {
            Log.e("MultiCameraManager", "Error vinculando cámara", e)
            null
        }
    }
    
    fun switchToCamera(cameraInfo: CameraInfo, lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        val cameraSelector = when (cameraInfo.facing) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        bindCamera(lifecycleOwner, cameraSelector, surfaceProvider)
    }
    
    fun captureImage(outputFileOptions: ImageCapture.OutputFileOptions, callback: ImageCapture.OnImageSavedCallback) {
        imageCapture?.takePicture(outputFileOptions, ContextCompat.getMainExecutor(context), callback)
    }
    
    fun setZoom(zoomRatio: Float) {
        currentCamera?.cameraControl?.setZoomRatio(zoomRatio)
    }
    
    fun setFlash(enabled: Boolean) {
        currentCamera?.cameraControl?.enableTorch(enabled)
    }
    
    fun getCameraInfo(cameraId: String): CameraInfo? {
        return availableCameras.find { it.id == cameraId }
    }
    
    fun getBestCameraForType(type: CameraType): CameraInfo? {
        return availableCameras.filter { it.type == type }
            .maxByOrNull { it.supportedResolutions.maxOfOrNull { size -> size.width * size.height } ?: 0 }
    }
    
    fun shutdown() {
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
