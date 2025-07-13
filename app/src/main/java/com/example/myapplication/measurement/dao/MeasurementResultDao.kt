package com.example.myapplication.measurement.dao

import androidx.room.*
import com.example.myapplication.measurement.MeasurementResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementResultDao {
    @Query("SELECT * FROM measurement_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<MeasurementResultEntity>>

    @Query("SELECT * FROM measurement_results WHERE id = :id")
    suspend fun getResultById(id: Long): MeasurementResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: MeasurementResultEntity): Long

    @Update
    suspend fun update(result: MeasurementResultEntity)

    @Delete
    suspend fun delete(result: MeasurementResultEntity)

    @Query("DELETE FROM measurement_results")
    suspend fun deleteAll()
}
