package com.example.myapplication.measurement

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "measurement_results")
data class MeasurementResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: Float,
    val unit: String,
    val confidence: Float,
    val method: String,
    val timestamp: Long
) 