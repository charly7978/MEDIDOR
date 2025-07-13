package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.dao.MeasurementDao
import com.example.myapplication.data.entity.MeasurementEntity
import com.example.myapplication.measurement.entity.MeasurementResultEntity
import com.example.myapplication.measurement.dao.MeasurementResultDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * Base de datos principal de la aplicación que contiene todas las entidades.
 * 
 * Esta clase define la configuración de la base de datos Room, incluyendo las entidades,
 * la versión y los DAOs disponibles.
 */
@Database(
    entities = [
        MeasurementEntity::class,
        MeasurementResultEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun measurementDao(): MeasurementDao
    abstract fun measurementResultDao(): MeasurementResultDao
    
    /**
     * Callback para la creación de la base de datos.
     * Se ejecuta cuando la base de datos se crea por primera vez.
     */
    private class AppDatabaseCallback @Inject constructor(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Aquí puedes realizar operaciones de inicialización
            scope.launch {
                // Ejemplo: Insertar datos por defecto
            }
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "measurement_database"
        
        /**
         * Obtiene una instancia de la base de datos.
         * Si no existe, la crea.
         * 
         * @param context Contexto de la aplicación
         * @return Instancia de AppDatabase
         */
        @Synchronized
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Inicialización en segundo plano
                        CoroutineScope(Dispatchers.IO).launch {
                            // Operaciones de inicialización
                        }
                    }
                })
                .apply {
                    if (BuildConfig.DEBUG) {
                        // Solo en modo debug
                        allowMainThreadQueries()
                    }
                }
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * Clase para la inyección de dependencias de Room.
     */
    class AppDatabaseFactory @Inject constructor(
        private val context: Context
    ) : Provider<AppDatabase> {
        override fun get(): AppDatabase {
            return getDatabase(context.applicationContext)
        }
    }
}
