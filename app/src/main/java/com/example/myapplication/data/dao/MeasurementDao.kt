package com.example.myapplication.data.dao

import androidx.room.*
import com.example.myapplication.data.entity.MeasurementEntity
import com.example.myapplication.measurement.entity.MeasurementType
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getMeasurementById(id: Long): MeasurementEntity?
    
    @Query("SELECT * FROM measurements WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteMeasurements(): Flow<List<MeasurementEntity>>
    
    @Query("SELECT DISTINCT type FROM measurements")
    suspend fun getMeasurementTypes(): List<String>
    
    @Query("SELECT DISTINCT sessionId FROM measurements WHERE sessionId IS NOT NULL")
    suspend fun getAllSessions(): List<String>
    
    @Query("SELECT * FROM measurements WHERE type = :type ORDER BY timestamp DESC")
    fun getMeasurementsByType(type: String): Flow<List<MeasurementEntity>>
    
    @Query("SELECT * FROM measurements WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getMeasurementsInTimeRange(startTime: Long, endTime: Long): Flow<List<MeasurementEntity>>
    
    @Query("SELECT * FROM measurements WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getMeasurementsBySession(sessionId: String): Flow<List<MeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: MeasurementEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(measurements: List<MeasurementEntity>)

    @Update
    suspend fun update(measurement: MeasurementEntity)
    
    @Update
    suspend fun updateAll(measurements: List<MeasurementEntity>)

    @Delete
    suspend fun delete(measurement: MeasurementEntity)
    
    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM measurements")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM measurements WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchMeasurements(query: String): Flow<List<MeasurementEntity>>
    
    @Query("UPDATE measurements SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
    
    @Query("SELECT * FROM measurements WHERE isSynced = 0")
    suspend fun getUnsyncedMeasurements(): List<MeasurementEntity>
    
    @Query("UPDATE measurements SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}
