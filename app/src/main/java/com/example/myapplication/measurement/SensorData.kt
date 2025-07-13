package com.example.myapplication.measurement

/**
 * Clase para almacenar y gestionar datos de sensores del dispositivo.
 * Proporciona una forma estructurada de manejar lecturas de sensores inerciales.
 */
data class SensorData(
    // Datos del acelerómetro (m/s²)
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    
    // Datos del giroscopio (rad/s)
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    
    // Datos del magnetómetro (μT)
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    
    // Presión barométrica (hPa)
    val pressure: Float = 1013.25f,
    
    // Temperatura ambiente (°C)
    val temperature: Float = 25.0f,
    
    // Tiempos de las últimas lecturas (nanosegundos)
    val lastAccelTimestamp: Long = 0L,
    val lastGyroTimestamp: Long = 0L,
    val lastMagTimestamp: Long = 0L,
    val lastPressureTimestamp: Long = 0L,
    
    // Precisión de los sensores
    val accelAccuracy: Int = 0,  // SENSOR_STATUS_*_ACCURACY
    val gyroAccuracy: Int = 0,
    val magAccuracy: Int = 0,
    
    // Estado de la calibración (0-1)
    val accelCalibration: Float = 1.0f,
    val gyroCalibration: Float = 1.0f,
    val magCalibration: Float = 1.0f
) {
    /**
     * Crea una copia actualizada con los nuevos datos del acelerómetro.
     */
    fun withAccelerometer(x: Float, y: Float, z: Float, timestamp: Long, accuracy: Int): SensorData {
        return copy(
            accelX = x,
            accelY = y,
            accelZ = z,
            lastAccelTimestamp = timestamp,
            accelAccuracy = accuracy
        )
    }
    
    /**
     * Crea una copia actualizada con los nuevos datos del giroscopio.
     */
    fun withGyroscope(x: Float, y: Float, z: Float, timestamp: Long, accuracy: Int): SensorData {
        return copy(
            gyroX = x,
            gyroY = y,
            gyroZ = z,
            lastGyroTimestamp = timestamp,
            gyroAccuracy = accuracy
        )
    }
    
    /**
     * Crea una copia actualizada con los nuevos datos del magnetómetro.
     */
    fun withMagnetometer(x: Float, y: Float, z: Float, timestamp: Long, accuracy: Int): SensorData {
        return copy(
            magX = x,
            magY = y,
            magZ = z,
            lastMagTimestamp = timestamp,
            magAccuracy = accuracy
        )
    }
    
    /**
     * Crea una copia actualizada con la nueva presión barométrica.
     */
    fun withPressure(pressure: Float, timestamp: Long): SensorData {
        return copy(
            pressure = pressure,
            lastPressureTimestamp = timestamp
        )
    }
    
    /**
     * Crea una copia actualizada con la nueva temperatura.
     */
    fun withTemperature(temp: Float): SensorData {
        return copy(temperature = temp)
    }
    
    /**
     * Verifica si los datos del sensor son recientes.
     * @param maxAgeMs Tiempo máximo en milisegundos para considerar los datos como válidos.
     */
    fun isDataFresh(sensorType: SensorType, maxAgeMs: Long = 1000L): Boolean {
        val currentTime = System.nanoTime()
        val timestamp = when (sensorType) {
            SensorType.ACCELEROMETER -> lastAccelTimestamp
            SensorType.GYROSCOPE -> lastGyroTimestamp
            SensorType.MAGNETOMETER -> lastMagTimestamp
            SensorType.PRESSURE -> lastPressureTimestamp
        }
        
        val ageNs = currentTime - timestamp
        val ageMs = ageNs / 1_000_000L
        
        return ageMs <= maxAgeMs
    }
    
    /**
     * Calcula la precisión general de los sensores (0-1).
     */
    fun getOverallAccuracy(): Float {
        val accelWeight = 0.4f
        val gyroWeight = 0.3f
        val magWeight = 0.3f
        
        val accelScore = when (accelAccuracy) {
            3 -> 1.0f  // SENSOR_STATUS_ACCURACY_HIGH
            2 -> 0.75f // SENSOR_STATUS_ACCURACY_MEDIUM
            1 -> 0.5f  // SENSOR_STATUS_ACCURACY_LOW
            else -> 0.25f // SENSOR_STATUS_UNRELIABLE o desconocido
        }
        
        val gyroScore = when (gyroAccuracy) {
            3 -> 1.0f
            2 -> 0.75f
            1 -> 0.5f
            else -> 0.25f
        }
        
        val magScore = when (magAccuracy) {
            3 -> 1.0f
            2 -> 0.75f
            1 -> 0.5f
            else -> 0.25f
        }
        
        return (accelScore * accelWeight + 
                gyroScore * gyroWeight + 
                magScore * magWeight) * 
               (accelCalibration * 0.4f + gyroCalibration * 0.3f + magCalibration * 0.3f)
    }
    
    /**
     * Tipos de sensores soportados.
     */
    enum class SensorType {
        ACCELEROMETER,
        GYROSCOPE,
        MAGNETOMETER,
        PRESSURE
    }
    
    companion object {
        /**
         * Crea una instancia con valores por defecto (sin lecturas).
         */
        fun empty(): SensorData = SensorData()
    }
}
