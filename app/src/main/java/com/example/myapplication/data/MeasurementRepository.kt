package com.example.myapplication.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entity.MeasurementEntity
import com.example.myapplication.measurement.MeasurementResult
import com.example.myapplication.measurement.MeasurementResultEntity
import com.example.myapplication.measurement.toMeasurementEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operaciones de base de datos de mediciones.
 */
@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<MeasurementEntity>>
    
    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getMeasurementById(id: Long): MeasurementEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: MeasurementEntity): Long
    
    @Update
    suspend fun update(measurement: MeasurementEntity)
    
    @Delete
    suspend fun delete(measurement: MeasurementEntity)
    
    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM measurements WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteMeasurements(): Flow<List<MeasurementEntity>>
    
    @Query("SELECT * FROM measurements WHERE type = :type ORDER BY timestamp DESC")
    fun getMeasurementsByType(type: String): Flow<List<MeasurementEntity>>
    
    @Query("SELECT DISTINCT type FROM measurements ORDER BY type")
    fun getMeasurementTypes(): Flow<List<String>>
    
    @Query("SELECT * FROM measurements WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getMeasurementsInTimeRange(startTime: Long, endTime: Long): Flow<List<MeasurementEntity>>
    
    @Query("SELECT * FROM measurements WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getMeasurementsBySession(sessionId: String): Flow<List<MeasurementEntity>>
    
    @Query("SELECT DISTINCT sessionId FROM measurements ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<String>>
}


/**
 * Repositorio para manejar operaciones de datos de mediciones.
 * Proporciona una API limpia para el resto de la aplicación.
 */
class MeasurementRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val measurementDao = db.measurementDao()
    // Flujo de todas las mediciones ordenadas por fecha descendente
    val allMeasurements: Flow<List<MeasurementEntity>> = measurementDao.getAllMeasurements()
    
    // Flujo de mediciones favoritas
    val favoriteMeasurements: Flow<List<MeasurementEntity>> = measurementDao.getFavoriteMeasurements()
    
    // Flujo de tipos de medición únicos
    val measurementTypes: Flow<List<String>> = measurementDao.getMeasurementTypes()
    
    // Flujo de IDs de sesión únicos
    val allSessions: Flow<List<String>> = measurementDao.getAllSessions()
    
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
    suspend fun insert(measurement: MeasurementEntity): Long {
        return measurementDao.insert(measurement)
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
