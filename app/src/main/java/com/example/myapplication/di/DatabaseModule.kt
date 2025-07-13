package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.dao.MeasurementDao
import com.example.myapplication.data.MeasurementRepository
import com.example.myapplication.measurement.dao.MeasurementResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias para la base de datos y sus componentes.
 * Proporciona instancias de la base de datos, DAOs y repositorios.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Proporciona una instancia de [AppDatabase] utilizando la fábrica.
     * 
     * @param context Contexto de la aplicación
     * @param factory Fábrica para crear la base de datos
     * @return Instancia de [AppDatabase]
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        factory: AppDatabase.AppDatabaseFactory
    ): AppDatabase {
        return factory.get()
    }
    
    /**
     * Proporciona una instancia de [MeasurementDao].
     * 
     * @param database Instancia de [AppDatabase]
     * @return Instancia de [MeasurementDao]
     */
    @Provides
    @Singleton
    fun provideMeasurementDao(database: AppDatabase): MeasurementDao {
        return database.measurementDao()
    }
    
    /**
     * Proporciona una instancia de [MeasurementResultDao].
     * 
     * @param database Instancia de [AppDatabase]
     * @return Instancia de [MeasurementResultDao]
     */
    @Provides
    @Singleton
    fun provideMeasurementResultDao(database: AppDatabase): MeasurementResultDao {
        return database.measurementResultDao()
    }
    
    /**
     * Proporciona una instancia de [MeasurementRepository].
     * 
     * @param measurementDao DAO para operaciones de medición
     * @param measurementResultDao DAO para operaciones de resultados de medición
     * @return Instancia de [MeasurementRepository]
     */
    @Provides
    @Singleton
    fun provideMeasurementRepository(
        measurementDao: MeasurementDao,
        measurementResultDao: MeasurementResultDao
    ): MeasurementRepository {
        return MeasurementRepository(measurementDao, measurementResultDao)
    }
    
    /**
     * Proporciona un [CoroutineScope] para operaciones en segundo plano.
     * 
     * @return [CoroutineScope] configurado con SupervisorJob
     */
    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }
}