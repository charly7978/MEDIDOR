package com.example.myapplication

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MeasurementApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("MeasurementApp", "Application started successfully")
        
        // Configuración adicional si es necesaria
        setupApplication()
    }
    
    private fun setupApplication() {
        // Configuraciones adicionales de la aplicación
        // Por ejemplo, configuración de logging, crash reporting, etc.
    }
}