package com.example.myapplication.measurement

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MeasurementResultEntity::class], version = 1)
abstract class MeasurementDatabase : RoomDatabase() {
    abstract fun measurementResultDao(): MeasurementResultDao
} 