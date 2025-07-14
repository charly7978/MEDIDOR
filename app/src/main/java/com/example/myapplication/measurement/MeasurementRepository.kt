package com.example.myapplication.measurement

import com.example.myapplication.data.dao.MeasurementDao
import com.example.myapplication.data.entity.MeasurementEntity
import com.example.myapplication.data.entity.MeasurementResultEntity
import com.example.myapplication.measurement.dao.MeasurementResultDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para gestionar operaciones relacionadas con mediciones.
 */
@Singleton
class MeasurementRepository @Inject constructor(
    private val measurementDao: MeasurementDao,
    private val measurementResultDao: MeasurementResultDao
) {
    /**
     * Inserta un nuevo resultado de medición.
     * 
     * @param result Resultado a insertar
     * @return ID del resultado insertado
     */
    suspend fun insert(result: MeasurementResultEntity): Long {
        return measurementResultDao.insert(result)
    }
    
    /**
     * Obtiene todos los resultados de medición.
     * 
     * @return Flow con la lista de resultados
     */
    fun getAll(): Flow<List<MeasurementResultEntity>> {
        return measurementResultDao.getAllResults()
    }
    
    /**
     * Elimina todos los resultados de medición.
     */
    suspend fun deleteAll() {
        measurementResultDao.deleteAllResults()
    }
    
    /**
     * Crea una nueva medición.
     * 
     * @param name Nombre de la medición
     * @param type Tipo de medición
     * @param calibrationFactor Factor de calibración
     * @return ID de la medición creada
     */
    suspend fun createMeasurement(
        name: String,
        type: String,
        calibrationFactor: Float = 1.0f
    ): Long {
        val now = Date()
        val measurement = MeasurementEntity(
            name = name,
            description = "Medición creada el ${now}",
            createdAt = now,
            updatedAt = now,
            type = type,
            calibrationFactor = calibrationFactor
        )
        return measurementDao.insert(measurement)
    }
}
