package com.example.myapplication.data

import androidx.room.TypeConverter
import java.util.Date

/**
 * Clase de conversores para Room.
 * Permite convertir tipos complejos como Date a tipos primitivos que Room puede almacenar.
 */
class Converters {
    /**
     * Convierte un timestamp (Long) a Date.
     * 
     * @param value Timestamp en milisegundos
     * @return Objeto Date correspondiente al timestamp, o null si el valor es null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Convierte un Date a timestamp (Long).
     * 
     * @param date Objeto Date a convertir
     * @return Timestamp en milisegundos, o null si el objeto Date es null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
