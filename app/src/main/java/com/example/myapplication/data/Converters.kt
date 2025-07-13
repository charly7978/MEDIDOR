package com.example.myapplication.data

import android.graphics.PointF
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Convertidores para tipos personalizados utilizados en las entidades de Room.
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromPointFList(points: List<PointF>): String {
        return gson.toJson(points)
    }
    
    @TypeConverter
    fun toPointFList(pointsString: String): List<PointF> {
        val listType = object : TypeToken<List<PointF>>() {}.type
        return gson.fromJson(pointsString, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }
    
    @TypeConverter
    fun toStringList(string: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(string, listType) ?: emptyList()
    }
}
