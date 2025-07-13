package com.example.myapplication.data

import android.content.Context
import androidx.room.Transaction
import com.example.myapplication.data.dao.MeasurementDao
import com.example.myapplication.data.entity.MeasurementEntity
import com.example.myapplication.measurement.entity.MeasurementResult
import com.example.myapplication.measurement.entity.MeasurementResultEntity
import com.example.myapplication.measurement.dao.MeasurementResultDao
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para manejar operaciones de datos de mediciones y resultados.
 * Proporciona una API limpia para el resto de la aplicación.
 */
@Singleton
class MeasurementRepository @Inject constructor(
    private val measurementDao: MeasurementDao,
    private val measurementResultDao: MeasurementResultDao
) {
    // Flujo de todas las mediciones ordenadas por fecha descendente
    val allMeasurements: Flow<List<MeasurementEntity>> = measurementDao.getAllMeasurements()
    
    // Flujo de mediciones favoritas
    val favoriteMeasurements: Flow<List<MeasurementEntity>> = measurementDao.getFavoriteMeasurements()
    
    // Flujo de tipos de medición únicos
    suspend fun getMeasurementTypes(): List<String> = measurementDao.getMeasurementTypes()
    
    // Flujo de IDs de sesión únicos
    suspend fun getAllSessions(): List<String> = measurementDao.getAllSessions()
    
    // Obtener mediciones por tipo
    fun getMeasurementsByType(type: String): Flow<List<MeasurementEntity>> {
        return measurementDao.getMeasurementsByType(type)
    }
    
    // Obtener mediciones por rango de tiempo
    fun getMeasurementsInTimeRange(startTime: Long, endTime: Long): Flow<List<MeasurementEntity>> {
        return measurementDao.getMeasurementsInTimeRange(startTime, endTime)
    }
    
    // Obtener mediciones por sesión
    fun getMeasurementsBySession(sessionId: String): Flow<List<MeasurementEntity>> {
        return measurementDao.getMeasurementsBySession(sessionId)
    }
    
    // Obtener una medición por ID
    suspend fun getMeasurementById(id: Long): MeasurementEntity? {
        return measurementDao.getMeasurementById(id)
    }
    
    // Insertar una nueva medición
    suspend fun insertMeasurement(measurement: MeasurementEntity): Long {
        return measurementDao.insert(measurement)
    }
    
    // Insertar múltiples mediciones
    suspend fun insertMeasurements(measurements: List<MeasurementEntity>) {
        measurementDao.insertAll(measurements)
    }
    
    // Actualizar una medición
    suspend fun updateMeasurement(measurement: MeasurementEntity) {
        measurementDao.update(measurement)
    }
    
    // Actualizar múltiples mediciones
    suspend fun updateMeasurements(measurements: List<MeasurementEntity>) {
        measurementDao.updateAll(measurements)
    }
    
    // Eliminar una medición
    suspend fun deleteMeasurement(measurement: MeasurementEntity) {
        measurementDao.delete(measurement)
    }
    
    // Eliminar una medición por ID
    suspend fun deleteMeasurementById(id: Long) {
        measurementDao.deleteById(id)
    }
    
    // Eliminar todas las mediciones
    suspend fun deleteAllMeasurements() {
        measurementDao.deleteAll()
    }
    
    // Buscar mediciones
    fun searchMeasurements(query: String): Flow<List<MeasurementEntity>> {
        return measurementDao.searchMeasurements(query)
    }
    
    // Actualizar estado de favorito
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean) {
        measurementDao.updateFavoriteStatus(id, isFavorite)
    }
    
    // Obtener resultados para una medición
    fun getResultsForMeasurement(measurementId: Long): Flow<List<MeasurementResultEntity>> {
        return measurementResultDao.getResultsForMeasurement(measurementId)
    }
    
    // Insertar un resultado de medición
    suspend fun insertResult(result: MeasurementResultEntity): Long {
        return measurementResultDao.insert(result)
    }
    
    // Insertar múltiples resultados
    suspend fun insertResults(results: List<MeasurementResultEntity>) {
        measurementResultDao.insertAll(results)
    }
    
    // Actualizar un resultado
    suspend fun updateResult(result: MeasurementResultEntity) {
        measurementResultDao.update(result)
    }
    
    // Eliminar un resultado
    suspend fun deleteResult(result: MeasurementResultEntity) {
        measurementResultDao.delete(result)
    }
    
    // Obtener todos los resultados
    fun getAllResults(): Flow<List<MeasurementResultEntity>> {
        return measurementResultDao.getAllResults()
    }
    
    // Obtener resultados recientes
    fun getRecentResults(limit: Int = 10): Flow<List<MeasurementResultEntity>> {
        return measurementResultDao.getRecentResults(limit)
    }
    
    // Obtener resultados por tipo
    fun getResultsByType(type: String): Flow<List<MeasurementResultEntity>> {
        return measurementResultDao.getResultsByType(type)
    }
    
    // Obtener resultados por rango de tiempo
    fun getResultsInTimeRange(startTime: Long, endTime: Long): Flow<List<MeasurementResultEntity>> {
        return measurementResultDao.getResultsInTimeRange(startTime, endTime)
    }
    
    // Transacción para guardar medición con sus resultados
    @Transaction
    suspend fun saveMeasurementWithResults(
        measurement: MeasurementEntity,
        results: List<MeasurementResultEntity>
    ) {
        val measurementId = measurementDao.insert(measurement)
        val resultsWithMeasurementId = results.map { it.copy(measurementId = measurementId) }
        measurementResultDao.insertAll(resultsWithMeasurementId)
    }
    
    // Obtener conteo total de resultados
    suspend fun getResultCount(): Int {
        return measurementResultDao.getResultCount()
    }
    
    // Marcar resultados como sincronizados
    suspend fun markResultsAsSynced(ids: List<Long>) {
        measurementResultDao.markAsSynced(ids)
    }
    
    // Obtener resultados no sincronizados
    suspend fun getUnsyncedResults(): List<MeasurementResultEntity> {
        return measurementResultDao.getUnsyncedResults()
    }
    
    // Insertar múltiples mediciones
    suspend fun insertAll(measurements: List<MeasurementEntity>) {
        measurements.forEach { measurementDao.insert(it) }
    }
    
    // Actualizar una medición existente
    suspend fun update(measurement: MeasurementEntity) {
        measurementDao.update(measurement)
    }
    
    // Eliminar una medición
    suspend fun delete(measurement: MeasurementEntity) {
        measurementDao.delete(measurement)
    }
    
    // Eliminar una medición por ID
    suspend fun deleteById(id: Long) {
        measurementDao.deleteById(id)
    }
    
    // Convertir de MeasurementResult a MeasurementEntity usando la extensión
    fun fromMeasurementResult(result: MeasurementResult, tags: List<String> = emptyList(), notes: String = ""): MeasurementEntity {
        return result.toMeasurementEntity(tags, notes)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: MeasurementRepository? = null
        
        fun getRepository(context: Context): MeasurementRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MeasurementRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
