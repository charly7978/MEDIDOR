package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.camera.CameraInfo
import com.example.myapplication.camera.MultiCameraManager
import com.example.myapplication.measurement.MeasurementEngine
import com.example.myapplication.measurement.MeasurementPoint
import com.example.myapplication.measurement.MeasurementResult
import com.example.myapplication.measurement.MeasurementType
import com.example.myapplication.sensors.SensorInfo
import com.example.myapplication.sensors.SensorManager
import com.example.myapplication.measurement.MeasurementRepository
import com.example.myapplication.measurement.MeasurementResultEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val measurementEngine: MeasurementEngine,
    private val sensorManager: SensorManager,
    private val cameraManager: MultiCameraManager,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {

    data class UiState(
        val hasPermissions: Boolean = false,
        val isCalibrated: Boolean = false,
        val availableCameras: List<CameraInfo> = emptyList(),
        val availableSensors: List<SensorInfo> = emptyList(),
        val features: List<Feature> = emptyList(),
        val measurementResults: List<MeasurementResult> = emptyList(),
        val currentPoints: List<MeasurementPoint> = emptyList(),
        val isMeasuring: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(error = exception.message) }
    }

    data class Feature(
        val title: String,
        val description: String
    )

    init {
        loadFeatures()
        initializeSensors()
        initializeCameras()
        observeMeasurements()
    }

    private fun loadFeatures() {
        _uiState.update { current ->
            current.copy(
                features = listOf(
                    Feature(
                        "Múltiples Cámaras", 
                        "Utiliza todas las cámaras disponibles del dispositivo"
                    ),
                    Feature(
                        "IA Integrada",
                        "TensorFlow Lite + ML Kit para detección automática"
                    ),
                    Feature(
                        "Sensores Completos",
                        "Fusión de múltiples sensores para máxima precisión"
                    ),
                    Feature(
                        "Mediciones AR 3D",
                        "ARCore para mediciones tridimensionales precisas"
                    ),
                    Feature(
                        "Calibración Automática",
                        "Detecta objetos conocidos para calibración"
                    ),
                    Feature(
                        "Análisis Avanzado",
                        "Indicadores de confianza y estabilidad"
                    )
                )
            )
        }
    }

    private fun initializeSensors() {
        viewModelScope.launch {
            sensorManager.startSensors()
            _uiState.update { current ->
                current.copy(
                    availableSensors = sensorManager.getAvailableSensors()
                )
            }
        }
    }

    private fun initializeCameras() {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    availableCameras = cameraManager.getAvailableCameras()
                )
            }
        }
    }

    private fun observeMeasurements() {
        viewModelScope.launch(errorHandler) {
            measurementRepository.getAll().collect { list ->
                _uiState.update { it.copy(measurementResults = list.map { it.toDomain() }) }
            }
        }
    }

    fun addMeasurementPoint(point: MeasurementPoint) {
        _uiState.update { current ->
            current.copy(
                currentPoints = current.currentPoints + point
            )
        }
    }

    fun addMeasurementResult(result: MeasurementResult) {
        viewModelScope.launch(errorHandler) {
            measurementRepository.insert(result.toEntity())
        }
    }

    fun clearCurrentPoints() {
        _uiState.update { current ->
            current.copy(currentPoints = emptyList())
        }
    }

    fun setMeasurementMode(mode: MeasurementType) {
        clearCurrentPoints()
        // TODO: Actualizar mode en el engine
    }

    fun setCalibrated(calibrated: Boolean) {
        _uiState.update { current ->
            current.copy(isCalibrated = calibrated)
        }
    }

    fun setPermissions(granted: Boolean) {
        _uiState.update { current ->
            current.copy(hasPermissions = granted)
        }
    }

    fun clearMeasurements() {
        viewModelScope.launch(errorHandler) {
            measurementRepository.deleteAll()
        }
    }

    fun exportMeasurements(): String {
        return _uiState.value.measurementResults.joinToString("\n") { result ->
            "${result.type.name}: ${result.value} ${result.unit} (${(result.confidence * 100).toInt()}% confianza)"
        }
    }

    fun setErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopSensors()
        cameraManager.release()
    }

    // Conversión entre Entity y Domain
    private fun MeasurementResultEntity.toDomain(): MeasurementResult = MeasurementResult(
        type = MeasurementType.valueOf(type),
        value = value,
        unit = unit,
        confidence = confidence,
        points = emptyList(), // No persistimos puntos por simplicidad
        method = method,
        timestamp = timestamp
    )
    private fun MeasurementResult.toEntity(): MeasurementResultEntity = MeasurementResultEntity(
        type = type.name,
        value = value,
        unit = unit,
        confidence = confidence,
        method = method,
        timestamp = timestamp
    )
}
