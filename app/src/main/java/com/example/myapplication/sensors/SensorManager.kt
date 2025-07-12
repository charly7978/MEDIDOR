package com.example.myapplication.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

data class SensorData(
    val acceleration: FloatArray = floatArrayOf(0f, 0f, 0f),
    val gyroscope: FloatArray = floatArrayOf(0f, 0f, 0f),
    val magnetometer: FloatArray = floatArrayOf(0f, 0f, 0f),
    val gravity: FloatArray = floatArrayOf(0f, 0f, 0f),
    val linearAcceleration: FloatArray = floatArrayOf(0f, 0f, 0f),
    val rotationVector: FloatArray = floatArrayOf(0f, 0f, 0f, 0f),
    val pressure: Float = 0f,
    val light: Float = 0f,
    val proximity: Float = 0f,
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val altitude: Float = 0f,
    val orientation: FloatArray = floatArrayOf(0f, 0f, 0f), // azimuth, pitch, roll
    val deviceTilt: Float = 0f,
    val compassHeading: Float = 0f
)

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Long = 0L
)

class AdvancedSensorManager(private val context: Context) : SensorEventListener, LocationListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()
    
    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData.asStateFlow()
    
    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated: StateFlow<Boolean> = _isCalibrated.asStateFlow()
    
    // Sensores disponibles
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    private var gravitySensor: Sensor? = null
    private var linearAccelerationSensor: Sensor? = null
    private var rotationVectorSensor: Sensor? = null
    private var pressureSensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var temperatureSensor: Sensor? = null
    private var humiditySensor: Sensor? = null
    
    // Variables para cálculos
    private val rotationMatrix = FloatArray(9)
    private val inclinationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    
    // Variables de calibración
    private var calibrationSamples = 0
    private val maxCalibrationSamples = 100
    private val accelerometerBaseline = FloatArray(3)
    private val gyroscopeBaseline = FloatArray(3)
    private val magnetometerBaseline = FloatArray(3)
    
    init {
        initializeSensors()
    }
    
    private fun initializeSensors() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        
        logAvailableSensors()
    }
    
    private fun logAvailableSensors() {
        val sensors = mutableListOf<String>()
        accelerometer?.let { sensors.add("Acelerómetro") }
        gyroscope?.let { sensors.add("Giroscopio") }
        magnetometer?.let { sensors.add("Magnetómetro") }
        gravitySensor?.let { sensors.add("Gravedad") }
        linearAccelerationSensor?.let { sensors.add("Aceleración Lineal") }
        rotationVectorSensor?.let { sensors.add("Vector de Rotación") }
        pressureSensor?.let { sensors.add("Presión") }
        lightSensor?.let { sensors.add("Luz Ambiente") }
        proximitySensor?.let { sensors.add("Proximidad") }
        temperatureSensor?.let { sensors.add("Temperatura") }
        humiditySensor?.let { sensors.add("Humedad") }
        
        Log.d("SensorManager", "Sensores disponibles: ${sensors.joinToString(", ")}")
    }
    
    fun startSensorUpdates() {
        val sensorDelay = AndroidSensorManager.SENSOR_DELAY_GAME
        
        accelerometer?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        gyroscope?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        magnetometer?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        gravitySensor?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        linearAccelerationSensor?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        rotationVectorSensor?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        pressureSensor?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        lightSensor?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        proximitySensor?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        temperatureSensor?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        humiditySensor?.let { 
            sensorManager.registerListener(this, it, sensorDelay)
        }
        
        startLocationUpdates()
    }
    
    private fun startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // 1 segundo
                0.5f, // 0.5 metros
                this
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000,
                0.5f,
                this
            )
        } catch (e: SecurityException) {
            Log.e("SensorManager", "Permisos de ubicación no concedidos", e)
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { 
            val currentData = _sensorData.value
            
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val newData = currentData.copy(acceleration = it.values.clone())
                    _sensorData.value = newData
                    updateCalibration(it.values, accelerometerBaseline)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    val newData = currentData.copy(gyroscope = it.values.clone())
                    _sensorData.value = newData
                    updateCalibration(it.values, gyroscopeBaseline)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    val newData = currentData.copy(magnetometer = it.values.clone())
                    _sensorData.value = newData
                    updateCalibration(it.values, magnetometerBaseline)
                    updateOrientation()
                }
                Sensor.TYPE_GRAVITY -> {
                    val newData = currentData.copy(gravity = it.values.clone())
                    _sensorData.value = newData
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    val newData = currentData.copy(linearAcceleration = it.values.clone())
                    _sensorData.value = newData
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    val newData = currentData.copy(rotationVector = it.values.clone())
                    _sensorData.value = newData
                }
                Sensor.TYPE_PRESSURE -> {
                    val altitude = calculateAltitudeFromPressure(it.values[0])
                    val newData = currentData.copy(pressure = it.values[0], altitude = altitude)
                    _sensorData.value = newData
                }
                Sensor.TYPE_LIGHT -> {
                    val newData = currentData.copy(light = it.values[0])
                    _sensorData.value = newData
                }
                Sensor.TYPE_PROXIMITY -> {
                    val newData = currentData.copy(proximity = it.values[0])
                    _sensorData.value = newData
                }
                Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                    val newData = currentData.copy(temperature = it.values[0])
                    _sensorData.value = newData
                }
                Sensor.TYPE_RELATIVE_HUMIDITY -> {
                    val newData = currentData.copy(humidity = it.values[0])
                    _sensorData.value = newData
                }
            }
        }
    }
    
    private fun updateOrientation() {
        val currentData = _sensorData.value
        if (currentData.acceleration.isNotEmpty() && currentData.magnetometer.isNotEmpty()) {
            val success = AndroidSensorManager.getRotationMatrix(
                rotationMatrix, inclinationMatrix,
                currentData.acceleration, currentData.magnetometer
            )
            
            if (success) {
                // Reorientar para dispositivo en modo retrato
                AndroidSensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    AndroidSensorManager.AXIS_X,
                    AndroidSensorManager.AXIS_Z,
                    remappedMatrix
                )
                
                AndroidSensorManager.getOrientation(remappedMatrix, orientation)
                
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                
                val compassHeading = (azimuth + 360) % 360
                val deviceTilt = sqrt(pitch * pitch + roll * roll)
                
                val newData = currentData.copy(
                    orientation = floatArrayOf(azimuth, pitch, roll),
                    compassHeading = compassHeading,
                    deviceTilt = deviceTilt
                )
                _sensorData.value = newData
            }
        }
    }
    
    private fun updateCalibration(values: FloatArray, baseline: FloatArray) {
        if (calibrationSamples < maxCalibrationSamples) {
            for (i in values.indices) {
                baseline[i] = (baseline[i] * calibrationSamples + values[i]) / (calibrationSamples + 1)
            }
            calibrationSamples++
            
            if (calibrationSamples >= maxCalibrationSamples) {
                _isCalibrated.value = true
                Log.d("SensorManager", "Calibración completada")
            }
        }
    }
    
    private fun calculateAltitudeFromPressure(pressure: Float): Float {
        // Fórmula barométrica estándar
        val seaLevelPressure = 1013.25f // hPa
        return (44330.0 * (1.0 - (pressure / seaLevelPressure).pow(0.1903))).toFloat()
    }
    
    fun getDistanceToObject(objectHeightInMeters: Float, pixelHeight: Int, imageHeight: Int, focalLength: Float): Float {
        // Calcular distancia usando trigonometría
        val sensorHeight = 5.76f // mm (ejemplo para sensor común)
        val realHeightOnSensor = (pixelHeight.toFloat() / imageHeight) * sensorHeight
        return (objectHeightInMeters * focalLength) / realHeightOnSensor * 1000 // convertir a metros
    }
    
    fun getDeviceStability(): Float {
        val currentData = _sensorData.value
        val gyroMagnitude = sqrt(
            currentData.gyroscope[0] * currentData.gyroscope[0] +
            currentData.gyroscope[1] * currentData.gyroscope[1] +
            currentData.gyroscope[2] * currentData.gyroscope[2]
        )
        
        // Retornar estabilidad como porcentaje (0 = muy inestable, 1 = muy estable)
        return max(0f, 1f - (gyroMagnitude / 2f))
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("SensorManager", "Precisión del sensor ${sensor?.name} cambió a: $accuracy")
    }
    
    override fun onLocationChanged(location: Location) {
        val newLocationData = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            bearing = location.bearing,
            speed = location.speed,
            timestamp = location.time
        )
        _locationData.value = newLocationData
    }
    
    fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
    }
    
    fun resetCalibration() {
        calibrationSamples = 0
        _isCalibrated.value = false
        accelerometerBaseline.fill(0f)
        gyroscopeBaseline.fill(0f)
        magnetometerBaseline.fill(0f)
    }
    
    fun getAvailableSensors(): List<String> {
        val available = mutableListOf<String>()
        sensorManager.getSensorList(Sensor.TYPE_ALL).forEach { sensor ->
            available.add("${sensor.name} (${sensor.vendor})")
        }
        return available
    }
}
