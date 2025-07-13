package com.example.myapplication.measurement.dao

import androidx.room.*
import com.example.myapplication.measurement.entity.MeasurementResultEntity
import com.example.myapplication.measurement.entity.MeasurementType
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface MeasurementResultDao {
    @Query("SELECT * FROM measurement_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<MeasurementResultEntity>>

    @Query("SELECT * FROM measurement_results WHERE id = :id")
    suspend fun getResultById(id: Long): MeasurementResultEntity?
    
    @Query("SELECT * FROM measurement_results WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteResults(): Flow<List<MeasurementResultEntity>>
    
    @Query("SELECT DISTINCT type FROM measurement_results")
    suspend fun getResultTypes(): List<String>
    
    @Query("SELECT DISTINCT sessionId FROM measurement_results WHERE sessionId IS NOT NULL")
    suspend fun getAllSessions(): List<String>
    
    @Query("SELECT * FROM measurement_results WHERE type = :type ORDER BY timestamp DESC")
    fun getResultsByType(type: String): Flow<List<MeasurementResultEntity>>
    
    @Query("SELECT * FROM measurement_results WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getResultsInTimeRange(startTime: Long, endTime: Long): Flow<List<MeasurementResultEntity>>
    
    @Query("SELECT * FROM measurement_results WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getResultsBySession(sessionId: String): Flow<List<MeasurementResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: MeasurementResultEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<MeasurementResultEntity>)

    @Update
    suspend fun update(result: MeasurementResultEntity)
    
    @Update
    suspend fun updateAll(results: List<MeasurementResultEntity>)

    @Delete
    suspend fun delete(result: MeasurementResultEntity)
    
    @Query("DELETE FROM measurement_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM measurement_results")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM measurement_results WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchResults(query: String): Flow<List<MeasurementResultEntity>>
    
    @Query("UPDATE measurement_results SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
    
    @Query("SELECT * FROM measurement_results WHERE isSynced = 0")
    suspend fun getUnsyncedResults(): List<MeasurementResultEntity>
    
    @Query("UPDATE measurement_results SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
    
    @Query("SELECT * FROM measurement_results WHERE measurementId = :measurementId")
    fun getResultsForMeasurement(measurementId: Long): Flow<List<MeasurementResultEntity>>
    
    @Query("SELECT COUNT(*) FROM measurement_results")
    suspend fun getResultCount(): Int
    
    @Query("SELECT * FROM measurement_results ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentResults(limit: Int): Flow<List<MeasurementResultEntity>>
}
