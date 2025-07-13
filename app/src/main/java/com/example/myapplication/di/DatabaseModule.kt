package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.measurement.MeasurementDatabase
import com.example.myapplication.measurement.MeasurementResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(appContext: Context): MeasurementDatabase =
        Room.databaseBuilder(
            appContext,
            MeasurementDatabase::class.java,
            "measurement_db"
        ).build()

    @Provides
    fun provideMeasurementResultDao(db: MeasurementDatabase): MeasurementResultDao =
        db.measurementResultDao()
} 