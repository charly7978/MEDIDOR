package com.example.myapplication.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

data class SensorData(
    val accelerometer: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val gyroscope: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magnetometer: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val barometer: Float = 0f,
    val light: Float = 0f,
    val proximity: Float = 0f,
    val location: Location? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CalibrationData(
    val accelerometerBias: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val gyroscopeBias: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magnetometerBias: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val barometerOffset: Float = 0f,
    val isCalibrated: Boolean = false
)

data class SensorInfo(
    val name: String,
    val type: String,
    val isAvailable: Boolean,
    val accuracy: String
)

class SensorManager(private val context: Context) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var currentSensorData = SensorData()
    private var calibrationData = CalibrationData()
    private var isCalibrating = false
    private var calibrationSamples = mutableListOf<SensorData>()

    // Filtros de Kalman para suavizar lecturas
    private val accelerometerFilter = KalmanFilter(3)
    private val gyroscopeFilter = KalmanFilter(3)
    private val magnetometerFilter = KalmanFilter(3)

    // Constantes físicas
    companion object {
        const val GRAVITY = 9.80665f // m/s²
        const val EARTH_RADIUS = 6371000f // metros
        const val CALIBRATION_SAMPLES = 100
        const val STABILITY_THRESHOLD = 0.1f
    }

    fun startSensors() {
        // Acelerómetro
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        // Giroscopio
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        // Magnetómetro
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        // Barómetro
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Sensor de luz
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Sensor de proximidad
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // GPS
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1 segundo
                1f, // 1 metro
                this
            )
        }
    }

    fun stopSensors() {
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
    }

    fun startCalibration() {
        isCalibrating = true
        calibrationSamples.clear()
    }

    fun stopCalibration(): CalibrationData {
        isCalibrating = false

        if (calibrationSamples.size >= CALIBRATION_SAMPLES) {
            calibrationData = calculateCalibrationData()
        }

        return calibrationData
    }

    private fun calculateCalibrationData(): CalibrationData {
        val accelX = calibrationSamples.map { it.accelerometer.first }.average().toFloat()
        val accelY = calibrationSamples.map { it.accelerometer.second }.average().toFloat()
        val accelZ = calibrationSamples.map { it.accelerometer.third }.average().toFloat()

        val gyroX = calibrationSamples.map { it.gyroscope.first }.average().toFloat()
        val gyroY = calibrationSamples.map { it.gyroscope.second }.average().toFloat()
        val gyroZ = calibrationSamples.map { it.gyroscope.third }.average().toFloat()

        val magX = calibrationSamples.map { it.magnetometer.first }.average().toFloat()
        val magY = calibrationSamples.map { it.magnetometer.second }.average().toFloat()
        val magZ = calibrationSamples.map { it.magnetometer.third }.average().toFloat()

        val baroOffset = calibrationSamples.map { it.barometer }.average().toFloat()

        return CalibrationData(
            accelerometerBias = Triple(accelX, accelY, accelZ - GRAVITY),
            gyroscopeBias = Triple(gyroX, gyroY, gyroZ),
            magnetometerBias = Triple(magX, magY, magZ),
            barometerOffset = baroOffset,
            isCalibrated = true
        )
    }

    fun getCurrentData(): SensorData = currentSensorData

    fun isDeviceStable(): Boolean {
        val accel = currentSensorData.accelerometer
        val gyro = currentSensorData.gyroscope

        val accelMagnitude =
            sqrt(accel.first * accel.first + accel.second * accel.second + accel.third * accel.third)
        val gyroMagnitude =
            sqrt(gyro.first * gyro.first + gyro.second * gyro.second + gyro.third * gyro.third)

        return abs(accelMagnitude - GRAVITY) < STABILITY_THRESHOLD && gyroMagnitude < STABILITY_THRESHOLD
    }

    fun calculateAltitude(pressure: Float): Float {
        // Fórmula barométrica para calcular altitud
        val seaLevelPressure = 101325f // Pa
        val temperature = 288.15f // K (15°C)
        val molarMass = 0.0289644f // kg/mol
        val gasConstant = 8.31447f // J/(mol·K)

        return -temperature * gasConstant / (molarMass * GRAVITY) * ln(pressure / seaLevelPressure)
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        // Fórmula de Haversine para calcular distancia entre dos puntos geográficos
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (EARTH_RADIUS * c).toFloat()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            val filteredValues = when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val filtered = accelerometerFilter.update(sensorEvent.values)
                    Triple(filtered[0], filtered[1], filtered[2])
                }

                Sensor.TYPE_GYROSCOPE -> {
                    val filtered = gyroscopeFilter.update(sensorEvent.values)
                    Triple(filtered[0], filtered[1], filtered[2])
                }

                Sensor.TYPE_MAGNETIC_FIELD -> {
                    val filtered = magnetometerFilter.update(sensorEvent.values)
                    Triple(filtered[0], filtered[1], filtered[2])
                }

                else -> Triple(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2])
            }

            currentSensorData = when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> currentSensorData.copy(accelerometer = filteredValues)
                Sensor.TYPE_GYROSCOPE -> currentSensorData.copy(gyroscope = filteredValues)
                Sensor.TYPE_MAGNETIC_FIELD -> currentSensorData.copy(magnetometer = filteredValues)
                Sensor.TYPE_PRESSURE -> currentSensorData.copy(barometer = sensorEvent.values[0])
                Sensor.TYPE_LIGHT -> currentSensorData.copy(light = sensorEvent.values[0])
                Sensor.TYPE_PROXIMITY -> currentSensorData.copy(proximity = sensorEvent.values[0])
                else -> currentSensorData
            }

            if (isCalibrating && calibrationSamples.size < CALIBRATION_SAMPLES) {
                calibrationSamples.add(currentSensorData)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Manejar cambios de precisión del sensor
    }

    override fun onLocationChanged(location: Location) {
        currentSensorData = currentSensorData.copy(location = location)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}

fun SensorManager.getAvailableSensors(): List<SensorInfo> {
    // Ejemplo: puedes mapear los sensores activos a SensorInfo
    return listOf(
        SensorInfo("Acelerómetro", "Movimiento", true, "Alta"),
        SensorInfo("Giroscopio", "Rotación", true, "Alta"),
        SensorInfo("Magnetómetro", "Brújula", true, "Media"),
        SensorInfo("Barómetro", "Presión", true, "Alta"),
        SensorInfo("GPS", "Ubicación", true, "Media"),
        SensorInfo("Sensor de Luz", "Ambiental", true, "Media"),
        SensorInfo("Proximidad", "Proximidad", true, "Alta")
    )
}

// Filtro de Kalman para suavizar lecturas de sensores
class KalmanFilter(private val dimension: Int) {
    private var x = FloatArray(dimension) // Estado estimado
    private var P = Array(dimension) { FloatArray(dimension) } // Matriz de covarianza
    private val Q = Array(dimension) { FloatArray(dimension) } // Ruido del proceso
    private val R = Array(dimension) { FloatArray(dimension) } // Ruido de medición

    init {
        // Inicializar matrices
        for (i in 0 until dimension) {
            P[i][i] = 1f
            Q[i][i] = 0.01f
            R[i][i] = 0.1f
        }
    }

    fun update(measurement: FloatArray): FloatArray {
        // Predicción
        val xPred = x.clone()
        val PPred = Array(dimension) { i ->
            FloatArray(dimension) { j ->
                P[i][j] + Q[i][j]
            }
        }

        // Actualización
        val K = Array(dimension) { FloatArray(dimension) } // Ganancia de Kalman
        for (i in 0 until dimension) {
            K[i][i] = PPred[i][i] / (PPred[i][i] + R[i][i])
        }

        // Actualizar estado
        for (i in 0 until dimension) {
            x[i] = xPred[i] + K[i][i] * (measurement[i] - xPred[i])
        }

        // Actualizar covarianza
        for (i in 0 until dimension) {
            for (j in 0 until dimension) {
                P[i][j] = (1f - K[i][i]) * PPred[i][j]
            }
        }

        return x.clone()
    }
} 