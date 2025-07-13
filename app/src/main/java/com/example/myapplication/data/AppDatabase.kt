package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.dao.MeasurementDao
import com.example.myapplication.data.entity.MeasurementEntity
import com.example.myapplication.data.entity.MeasurementResultEntity
import com.example.myapplication.measurement.dao.MeasurementResultDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Base de datos principal de la aplicación utilizando Room.
 * Contiene las tablas para mediciones y resultados de mediciones.
 */
@Database(
    entities = [
        MeasurementEntity::class,
        MeasurementResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Proporciona acceso al DAO de mediciones.
     */
    abstract fun measurementDao(): MeasurementDao
    
    /**
     * Proporciona acceso al DAO de resultados de mediciones.
     */
    abstract fun measurementResultDao(): MeasurementResultDao
    
    /**
     * Fábrica para crear instancias de la base de datos.
     * Utiliza el patrón Singleton para garantizar una única instancia.
     */
    @Singleton
    class AppDatabaseFactory @Inject constructor(
        @ApplicationContext private val context: Context
    ) {
        /**
         * Obtiene la instancia de la base de datos.
         */
        fun get(): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "measurement_database"
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}