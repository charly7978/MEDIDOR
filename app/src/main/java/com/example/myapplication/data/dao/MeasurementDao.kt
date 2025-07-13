package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones relacionadas con mediciones.
 */
@Dao
interface MeasurementDao {
    
    /**
     * Inserta una nueva medición en la base de datos.
     * 
     * @param measurement Medición a insertar
     * @return ID de la medición insertada
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: MeasurementEntity): Long
    
    /**
     * Actualiza una medición existente.
     * 
     * @param measurement Medición a actualizar
     */
    @Update
    suspend fun update(measurement: MeasurementEntity)
    
    /**
     * Elimina una medición.
     * 
     * @param measurement Medición a eliminar
     */
    @Delete
    suspend fun delete(measurement: MeasurementEntity)
    
    /**
     * Obtiene todas las mediciones.
     * 
     * @return Flow con la lista de mediciones
     */
    @Query("SELECT * FROM measurements ORDER BY createdAt DESC")
    fun getAllMeasurements(): Flow<List<MeasurementEntity>>
    
    /**
     * Obtiene una medición por su ID.
     * 
     * @param id ID de la medición
     * @return Medición correspondiente al ID
     */
    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getMeasurementById(id: Long): MeasurementEntity?
    
    /**
     * Obtiene mediciones por tipo.
     * 
     * @param type Tipo de medición
     * @return Flow con la lista de mediciones del tipo especificado
     */
    @Query("SELECT * FROM measurements WHERE type = :type ORDER BY createdAt DESC")
    fun getMeasurementsByType(type: String): Flow<List<MeasurementEntity>>
    
    /**
     * Elimina todas las mediciones.
     */
    @Query("DELETE FROM measurements")
    suspend fun deleteAllMeasurements()
}
