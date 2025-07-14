package com.example.myapplication.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Información sobre un sensor disponible en el dispositivo.
 */
data class SensorInfo(
    val name: String,
    val type: Int,
    val vendor: String,
    val version: Int,
    val resolution: Float,
    val range: Float,
    val isAvailable: Boolean = true
)

/**
 * Administrador de sensores que proporciona acceso a los sensores del dispositivo.
 */
@Singleton
class SensorManager @Inject constructor(
    private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val activeSensors = mutableMapOf<Int, Sensor>()
    private val sensorData = mutableMapOf<Int, FloatArray>()
    
    companion object {
        private const val TAG = "SensorManager"
    }
    
    /**
     * Inicia la recolección de datos de los sensores.
     */
    fun startSensors() {
        try {
            // Registrar sensores principales
            registerSensor(Sensor.TYPE_ACCELEROMETER)
            registerSensor(Sensor.TYPE_GYROSCOPE)
            registerSensor(Sensor.TYPE_MAGNETIC_FIELD)
            registerSensor(Sensor.TYPE_GRAVITY)
            
            // Intentar registrar sensores adicionales si están disponibles
            registerSensor(Sensor.TYPE_ROTATION_VECTOR)
            registerSensor(Sensor.TYPE_PRESSURE)
            registerSensor(Sensor.TYPE_LIGHT)
            
            Log.d(TAG, "Sensores iniciados: ${activeSensors.size} sensores activos")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar sensores", e)
        }
    }
    
    /**
     * Detiene la recolección de datos de los sensores.
     */
    fun stopSensors() {
        try {
            sensorManager.unregisterListener(this)
            activeSensors.clear()
            Log.d(TAG, "Sensores detenidos")
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener sensores", e)
        }
    }
    
    /**
     * Registra un sensor para recibir actualizaciones.
     */
    private fun registerSensor(sensorType: Int) {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor != null) {
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            activeSensors[sensorType] = sensor
            Log.d(TAG, "Sensor registrado: ${sensor.name}")
        }
    }
    
    /**
     * Obtiene los datos más recientes de un sensor específico.
     */
    fun getSensorData(sensorType: Int): FloatArray? {
        return sensorData[sensorType]
    }
    
    /**
     * Obtiene una lista de todos los sensores disponibles en el dispositivo.
     */
    fun getAvailableSensors(): List<SensorInfo> {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        return sensors.map { sensor ->
            SensorInfo(
                name = sensor.name,
                type = sensor.type,
                vendor = sensor.vendor,
                version = sensor.version,
                resolution = sensor.resolution,
                range = sensor.maximumRange
            )
        }
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        // Guardar los datos del sensor
        sensorData[event.sensor.type] = event.values.clone()
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No se requiere implementación
    }
}