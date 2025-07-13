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
import kotlinx.coroutines.launch
import kotlin.math.*

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {

    private val measurementEngine = MeasurementEngine()
    private val sensorManager = SensorManager(application)
    private val cameraManager = MultiCameraManager(application)

    // Estados de la UI
    private val _measurementResults = MutableStateFlow<List<MeasurementResult>>(emptyList())
    val measurementResults: StateFlow<List<MeasurementResult>> = _measurementResults.asStateFlow()

    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated: StateFlow<Boolean> = _isCalibrated.asStateFlow()

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    private val _currentPoints = MutableStateFlow<List<MeasurementPoint>>(emptyList())
    val currentPoints: StateFlow<List<MeasurementPoint>> = _currentPoints.asStateFlow()

    private val _sensorData = MutableStateFlow(sensorManager.getCurrentData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _availableCameras = MutableStateFlow(cameraManager.getAvailableCameras())
    val availableCameras: StateFlow<List<CameraInfo>> = _availableCameras.asStateFlow()

    // Configuración de medición
    private var measurementMode = MeasurementType.DISTANCE
    private var selectedCamera = 0

    init {
        initializeSensors()
        initializeCameras()
    }

    private fun initializeSensors() {
        viewModelScope.launch {
            sensorManager.startSensors()
            // Actualizar datos de sensores periódicamente
            while (true) {
                _sensorData.value = sensorManager.getCurrentData()
                kotlinx.coroutines.delay(100) // 10 FPS
            }
        }
    }

    private fun initializeCameras() {
        viewModelScope.launch {
            // La inicialización de cámaras se hace en la UI
            _availableCameras.value = cameraManager.getAvailableCameras()
        }
    }

    fun startCalibration() {
        viewModelScope.launch {
            sensorManager.startCalibration()
            _isCalibrated.value = false

            // Simular proceso de calibración
            kotlinx.coroutines.delay(3000) // 3 segundos

            val calibrationData = sensorManager.stopCalibration()
            _isCalibrated.value = calibrationData.isCalibrated

            if (calibrationData.isCalibrated) {
                // Aplicar factores de calibración al motor de medición
                applyCalibrationFactors(calibrationData)
            }
        }
    }

    private fun applyCalibrationFactors(calibrationData: CalibrationData) {
        // Aplicar correcciones de sesgo a las mediciones
        // Esto se implementaría en el motor de medición
    }

    fun addMeasurementPoint(x: Float, y: Float, z: Float = 0f) {
        val point = MeasurementPoint(
            x = x,
            y = y,
            z = z,
            confidence = calculatePointConfidence(x, y, z)
        )

        _currentPoints.value = _currentPoints.value + point

        // Si tenemos suficientes puntos, realizar medición
        when (measurementMode) {
            MeasurementType.DISTANCE -> {
                if (_currentPoints.value.size >= 2) {
                    performDistanceMeasurement()
                }
            }

            MeasurementType.AREA -> {
                if (_currentPoints.value.size >= 3) {
                    performAreaMeasurement()
                }
            }

            MeasurementType.ANGLE -> {
                if (_currentPoints.value.size >= 3) {
                    performAngleMeasurement()
                }
            }

            else -> {
                // Otros tipos de medición
            }
        }
    }

    private fun calculatePointConfidence(x: Float, y: Float, z: Float): Float {
        var confidence = 1.0f

        // Verificar estabilidad del dispositivo
        if (!sensorManager.isDeviceStable()) {
            confidence *= 0.7f
        }

        // Verificar calidad de la imagen (basado en sensores)
        val lightLevel = _sensorData.value.light
        if (lightLevel < 50f) { // Poca luz
            confidence *= 0.8f
        }

        // Verificar proximidad (si el objeto está muy cerca o muy lejos)
        val proximity = _sensorData.value.proximity
        if (proximity > 0 && proximity < 5f) { // Muy cerca
            confidence *= 0.9f
        }

        return confidence.coerceIn(0f, 1f)
    }

    private fun performDistanceMeasurement() {
        val points = _currentPoints.value
        if (points.size >= 2) {
            val result = measurementEngine.measureDistance(points[0], points[1])
            addMeasurementResult(result)
            clearCurrentPoints()
        }
    }

    private fun performAreaMeasurement() {
        val points = _currentPoints.value
        if (points.size >= 3) {
            try {
                val result = measurementEngine.measureArea(points)
                addMeasurementResult(result)
                clearCurrentPoints()
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    private fun performAngleMeasurement() {
        val points = _currentPoints.value
        if (points.size >= 3) {
            val result = measurementEngine.measureAngle(points[0], points[1], points[2])
            addMeasurementResult(result)
            clearCurrentPoints()
        }
    }

    private fun addMeasurementResult(result: MeasurementResult) {
        _measurementResults.value = _measurementResults.value + result
    }

    fun clearCurrentPoints() {
        _currentPoints.value = emptyList()
    }

    fun clearAllMeasurements() {
        _measurementResults.value = emptyList()
        clearCurrentPoints()
    }

    fun setMeasurementMode(mode: MeasurementType) {
        measurementMode = mode
        clearCurrentPoints()
    }

    fun setSelectedCamera(cameraIndex: Int) {
        selectedCamera = cameraIndex
    }

    fun startMeasurement() {
        _isMeasuring.value = true
    }

    fun stopMeasurement() {
        _isMeasuring.value = false
        clearCurrentPoints()
    }

    fun exportMeasurements(): String {
        val results = _measurementResults.value
        if (results.isEmpty()) return "No hay mediciones para exportar"

        val csv = StringBuilder()
        csv.append("Tipo,Valor,Unidad,Confianza,Método,Timestamp\n")

        results.forEach { result ->
            csv.append("${result.type},${result.value},${result.unit},${result.confidence},${result.method},${result.timestamp}\n")
        }

        return csv.toString()
    }

    fun getMeasurementStatistics(): MeasurementStatistics {
        val results = _measurementResults.value
        if (results.isEmpty()) {
            return MeasurementStatistics()
        }

        val distances = results.filter { it.type == MeasurementType.DISTANCE }
        val areas = results.filter { it.type == MeasurementType.AREA }
        val angles = results.filter { it.type == MeasurementType.ANGLE }

        return MeasurementStatistics(
            totalMeasurements = results.size,
            averageConfidence = results.map { it.confidence }.average().toFloat(),
            distanceCount = distances.size,
            areaCount = areas.size,
            angleCount = angles.size,
            totalDistance = distances.sumOf { it.value.toDouble() }.toFloat(),
            totalArea = areas.sumOf { it.value.toDouble() }.toFloat()
        )
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopSensors()
        cameraManager.release()
    }
}

data class MeasurementStatistics(
    val totalMeasurements: Int = 0,
    val averageConfidence: Float = 0f,
    val distanceCount: Int = 0,
    val areaCount: Int = 0,
    val angleCount: Int = 0,
    val totalDistance: Float = 0f,
    val totalArea: Float = 0f
) 