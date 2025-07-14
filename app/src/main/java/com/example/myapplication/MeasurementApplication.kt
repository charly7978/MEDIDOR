package com.example.myapplication

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MeasurementApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("MeasurementApp", "Application started successfully")
        
        setupApplication()
    }
    
    private fun setupApplication() {
        // Additional application configurations
        // For example, logging setup, crash reporting, etc.
    }
}