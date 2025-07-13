package com.example.myapplication.data

import com.example.myapplication.data.dao.MeasurementDao
import com.example.myapplication.data.entity.MeasurementEntity
import com.example.myapplication.data.entity.MeasurementResultEntity
import com.example.myapplication.measurement.dao.MeasurementResultDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para gestionar operaciones relacionadas con mediciones y sus resultados.
 * Proporciona una capa de abstracción sobre los DAOs.
 */
@Singleton
class MeasurementRepository @Inject constructor(
    private val measurementDao: MeasurementDao,
    private val measurementResultDao: MeasurementResultDao
) {
    
    // Operaciones para mediciones
    
    /**
     * Inserta una nueva medición.
     * 
     * @param measurement Medición a insertar
     * @return ID de la medición insertada
     */
    suspend fun insertMeasurement(measurement: MeasurementEntity): Long {
        return measurementDao.insert(measurement)
    }
    
    /**
     * Actualiza una medición existente.
     * 
     * @param measurement Medición a actualizar
     */
    suspend fun updateMeasurement(measurement: MeasurementEntity) {
        measurementDao.update(measurement)
    }
    
    /**
     * Elimina una medición.
     * 
     * @param measurement Medición a eliminar
     */
    suspend fun deleteMeasurement(measurement: MeasurementEntity) {
        measurementDao.delete(measurement)
    }
    
    /**
     * Obtiene todas las mediciones.
     * 
     * @return Flow con la lista de mediciones
     */
    fun getAllMeasurements(): Flow<List<MeasurementEntity>> {
        return measurementDao.getAllMeasurements()
    }
    
    /**
     * Obtiene una medición por su ID.
     * 
     * @param id ID de la medición
     * @return Medición correspondiente al ID
     */
    suspend fun getMeasurementById(id: Long): MeasurementEntity? {
        return measurementDao.getMeasurementById(id)
    }
    
    /**
     * Obtiene mediciones por tipo.
     * 
     * @param type Tipo de medición
     * @return Flow con la lista de mediciones del tipo especificado
     */
    fun getMeasurementsByType(type: String): Flow<List<MeasurementEntity>> {
        return measurementDao.getMeasurementsByType(type)
    }
    
    /**
     * Elimina todas las mediciones.
     */
    suspend fun deleteAllMeasurements() {
        measurementDao.deleteAllMeasurements()
    }
    
    // Operaciones para resultados de mediciones
    
    /**
     * Inserta un nuevo resultado de medición.
     * 
     * @param result Resultado a insertar
     * @return ID del resultado insertado
     */
    suspend fun insertResult(result: MeasurementResultEntity): Long {
        return measurementResultDao.insert(result)
    }
    
    /**
     * Actualiza un resultado de medición existente.
     * 
     * @param result Resultado a actualizar
     */
    suspend fun updateResult(result: MeasurementResultEntity) {
        measurementResultDao.update(result)
    }
    
    /**
     * Elimina un resultado de medición.
     * 
     * @param result Resultado a eliminar
     */
    suspend fun deleteResult(result: MeasurementResultEntity) {
        measurementResultDao.delete(result)
    }
    
    /**
     * Obtiene todos los resultados de medición.
     * 
     * @return Flow con la lista de resultados
     */
    fun getAllResults(): Flow<List<MeasurementResultEntity>> {
        return measurementResultDao.getAllResults()
    }
    
    /**
     * Obtiene resultados por ID de medición.
     * 
     * @param measurementId ID de la medición
     * @return Flow con la lista de resultados para la medición especificada
     */
    fun getResultsByMeasurementId(measurementId: Long): Flow<List<MeasurementResultEntity>> {
        return measurementResultDao.getResultsByMeasurementId(measurementId)
    }
    
    /**
     * Obtiene un resultado por su ID.
     * 
     * @param id ID del resultado
     * @return Resultado correspondiente al ID
     */
    suspend fun getResultById(id: Long): MeasurementResultEntity? {
        return measurementResultDao.getResultById(id)
    }
    
    /**
     * Elimina todos los resultados.
     */
    suspend fun deleteAllResults() {
        measurementResultDao.deleteAllResults()
    }
    
    /**
     * Elimina resultados por ID de medición.
     * 
     * @param measurementId ID de la medición
     */
    suspend fun deleteResultsByMeasurementId(measurementId: Long) {
        measurementResultDao.deleteResultsByMeasurementId(measurementId)
    }
}
