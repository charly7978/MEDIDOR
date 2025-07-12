package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.camera.MultiCameraManager
import com.example.myapplication.measurement.MeasurementEngine
import com.example.myapplication.measurement.MeasurementResult
import com.example.myapplication.measurement.MeasurementType
import com.example.myapplication.sensors.AdvancedSensorManager
import com.example.myapplication.sensors.SensorData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MeasurementMode {
    DISTANCE, ANGLE, AREA, HEIGHT, OBJECT_DETECTION, CALIBRATION
}

data class UiState(
    val isInitialized: Boolean = false,
    val currentMode: MeasurementMode = MeasurementMode.DISTANCE,
    val measurementResults: List<MeasurementResult> = emptyList(),
    val sensorData: SensorData = SensorData(),
    val isCalibrated: Boolean = false,
    val availableCameras: List<String> = emptyList(),
    val currentCameraIndex: Int = 0,
    val showSettings: Boolean = false,
    val errorMessage: String? = null,
    val isProcessing: Boolean = false
)

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {
    
    private val cameraManager = MultiCameraManager(application)
    private val sensorManager = AdvancedSensorManager(application)
    private val measurementEngine = MeasurementEngine(application)
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        initializeManagers()
        observeDataStreams()
    }
    
    private fun initializeManagers() {
        viewModelScope.launch {
            try {
                // Inicializar gestores
                cameraManager.initializeCameras()
                sensorManager.startSensorUpdates()
                
                // Actualizar estado
                _uiState.value = _uiState.value.copy(
                    isInitialized = true,
                    availableCameras = cameraManager.availableCameras.map { "${it.type.name} - ${it.id}" }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error inicializando: ${e.message}"
                )
            }
        }
    }
    
    private fun observeDataStreams() {
        // Observar datos de sensores
        viewModelScope.launch {
            sensorManager.sensorData.collect { sensorData ->
                _uiState.value = _uiState.value.copy(sensorData = sensorData)
            }
        }
        
        // Observar estado de calibración
        viewModelScope.launch {
            measurementEngine.isCalibrated.collect { isCalibrated ->
                _uiState.value = _uiState.value.copy(isCalibrated = isCalibrated)
            }
        }
        
        // Observar resultados de medición
        viewModelScope.launch {
            measurementEngine.measurementResults.collect { results ->
                _uiState.value = _uiState.value.copy(measurementResults = results)
            }
        }
    }
    
    fun processImage(imageProxy: ImageProxy) {
        if (!_uiState.value.isProcessing) {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            viewModelScope.launch {
                try {
                    measurementEngine.processImage(imageProxy, _uiState.value.sensorData)
                } finally {
                    _uiState.value = _uiState.value.copy(isProcessing = false)
                }
            }
        }
    }
    
    fun setMeasurementMode(mode: MeasurementMode) {
        _uiState.value = _uiState.value.copy(currentMode = mode)
        measurementEngine.clearMeasurementPoints()
    }
    
    fun addMeasurementPoint(x: Float, y: Float, imageWidth: Int, imageHeight: Int) {
        measurementEngine.addMeasurementPoint(x, y, imageWidth, imageHeight)
    }
    
    fun clearMeasurementPoints() {
        measurementEngine.clearMeasurementPoints()
    }
    
    fun undoLastPoint() {
        measurementEngine.undoLastPoint()
    }
    
    fun switchCamera(index: Int) {
        if (index < cameraManager.availableCameras.size) {
            _uiState.value = _uiState.value.copy(currentCameraIndex = index)
            // Lógica para cambiar cámara se manejará en la UI
        }
    }
    
    fun manualCalibration(knownDistanceMm: Float, pixelDistance: Float) {
        measurementEngine.manualCalibration(knownDistanceMm, pixelDistance)
    }
    
    fun toggleSettings() {
        _uiState.value = _uiState.value.copy(showSettings = !_uiState.value.showSettings)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun exportMeasurements(): String {
        return measurementEngine.exportMeasurements()
    }
    
    fun getSensorStability(): Float {
        return sensorManager.getDeviceStability()
    }
    
    fun getCameraManager() = cameraManager
    fun getSensorManager() = sensorManager
    fun getMeasurementEngine() = measurementEngine
    
    override fun onCleared() {
        super.onCleared()
        sensorManager.stopSensorUpdates()
        measurementEngine.cleanup()
        cameraManager.shutdown()
    }
}
