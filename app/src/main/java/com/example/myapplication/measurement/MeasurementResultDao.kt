package com.example.myapplication.measurement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementResultDao {
    @Insert
    suspend fun insert(result: MeasurementResultEntity): Long

    @Query("SELECT * FROM measurement_results ORDER BY timestamp DESC")
    fun getAll(): Flow<List<MeasurementResultEntity>>

    @Query("DELETE FROM measurement_results")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(result: MeasurementResultEntity)
} 