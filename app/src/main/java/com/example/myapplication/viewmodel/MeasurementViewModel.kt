package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.camera.MultiCameraManager
import com.example.myapplication.measurement.MeasurementEngine
import com.example.myapplication.measurement.MeasurementPoint
import com.example.myapplication.measurement.MeasurementResult
import com.example.myapplication.measurement.MeasurementType
import com.example.myapplication.sensors.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {

    private val measurementEngine = MeasurementEngine()
    private val sensorManager = SensorManager(application)
    private val cameraManager = MultiCameraManager(application)

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

    data class Feature(
        val title: String,
        val description: String
    )

    init {
        loadFeatures()
        initializeSensors()
        initializeCameras()
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

    fun addMeasurementPoint(point: MeasurementPoint) {
        _uiState.update { current ->
            current.copy(
                currentPoints = current.currentPoints + point
            )
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

    // Resto de funciones implementadas de manera similar...
    // (PerformedMeasurement, exportMeasurements, etc.)

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopSensors()
        cameraManager.release()
    }
}
