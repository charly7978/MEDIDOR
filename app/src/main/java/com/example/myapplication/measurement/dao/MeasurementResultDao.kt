package com.example.myapplication.measurement.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entity.MeasurementResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones relacionadas con resultados de mediciones.
 */
@Dao
interface MeasurementResultDao {
    
    /**
     * Inserta un nuevo resultado de medición en la base de datos.
     * 
     * @param result Resultado a insertar
     * @return ID del resultado insertado
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: MeasurementResultEntity): Long
    
    /**
     * Actualiza un resultado de medición existente.
     * 
     * @param result Resultado a actualizar
     */
    @Update
    suspend fun update(result: MeasurementResultEntity)
    
    /**
     * Elimina un resultado de medición.
     * 
     * @param result Resultado a eliminar
     */
    @Delete
    suspend fun delete(result: MeasurementResultEntity)
    
    /**
     * Obtiene todos los resultados de medición.
     * 
     * @return Flow con la lista de resultados
     */
    @Query("SELECT * FROM measurement_results ORDER BY createdAt DESC")
    fun getAllResults(): Flow<List<MeasurementResultEntity>>
    
    /**
     * Obtiene resultados por ID de medición.
     * 
     * @param measurementId ID de la medición
     * @return Flow con la lista de resultados para la medición especificada
     */
    @Query("SELECT * FROM measurement_results WHERE measurementId = :measurementId ORDER BY createdAt DESC")
    fun getResultsByMeasurementId(measurementId: Long): Flow<List<MeasurementResultEntity>>
    
    /**
     * Obtiene un resultado por su ID.
     * 
     * @param id ID del resultado
     * @return Resultado correspondiente al ID
     */
    @Query("SELECT * FROM measurement_results WHERE id = :id")
    suspend fun getResultById(id: Long): MeasurementResultEntity?
    
    /**
     * Elimina todos los resultados.
     */
    @Query("DELETE FROM measurement_results")
    suspend fun deleteAllResults()
    
    /**
     * Elimina resultados por ID de medición.
     * 
     * @param measurementId ID de la medición
     */
    @Query("DELETE FROM measurement_results WHERE measurementId = :measurementId")
    suspend fun deleteResultsByMeasurementId(measurementId: Long)
}
