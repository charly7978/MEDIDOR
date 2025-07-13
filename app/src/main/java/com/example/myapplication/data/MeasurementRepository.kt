package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapplication.measurement.MeasurementResult
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * Entidad de base de datos para almacenar mediciones.
 */
@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: Double,
    val unit: String,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val points: List<PointF> = emptyList(),
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val isFavorite: Boolean = false,
    val calibrationFactor: Double = 1.0,
    val deviceId: String = "",
    val sessionId: String = UUID.randomUUID().toString()
)

/**
 * DAO (Data Access Object) para operaciones de base de datos de mediciones.
 */
@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<MeasurementEntity>>
    
    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getMeasurementById(id: Long): MeasurementEntity?
    
    @Insert
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
    
    @Query("SELECT * FROM measurements WHERE sessionId = :sessionId ORDER BY timestamp")
    fun getMeasurementsBySession(sessionId: String): Flow<List<MeasurementEntity>>
    
    @Query("SELECT DISTINCT sessionId FROM measurements ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<String>>
}

/**
 * Convertidores para tipos personalizados.
 */
class Converters {
    @TypeConverter
    fun fromPointFList(points: List<PointF>): String {
        return points.joinToString("|") { "${it.x},${it.y}" }
    }
    
    @TypeConverter
    fun toPointFList(pointsString: String): List<PointF> {
        if (pointsString.isEmpty()) return emptyList()
        return pointsString.split("|").map { pointStr ->
            val (x, y) = pointStr.split(",")
            PointF(x.toFloat(), y.toFloat())
        }
    }
    
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString("|")
    }
    
    @TypeConverter
    fun toStringList(string: String): List<String> {
        return if (string.isEmpty()) emptyList() else string.split("|")
    }
}

/**
 * Base de datos de Room para almacenar mediciones.
 */
@Database(entities = [MeasurementEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "measurement_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Repositorio para manejar operaciones de datos de mediciones.
 * Proporciona una API limpia para el resto de la aplicación.
 */
class MeasurementRepository private constructor(private val measurementDao: MeasurementDao) {
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
    
    // Convertir de MeasurementResult a MeasurementEntity
    fun fromMeasurementResult(result: MeasurementResult, tags: List<String> = emptyList(), notes: String = ""): MeasurementEntity {
        return MeasurementEntity(
            type = result.type.name,
            value = result.value,
            unit = result.unit,
            confidence = result.confidence,
            points = result.points,
            tags = tags,
            notes = notes,
            calibrationFactor = result.calibrationFactor
        )
    }
    
    companion object {
        @Volatile
        private var INSTANCE: MeasurementRepository? = null
        
        fun getRepository(dao: MeasurementDao): MeasurementRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MeasurementRepository(dao)
                INSTANCE = instance
                instance
            }
        }
    }
}
