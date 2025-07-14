package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.camera.CameraInfo
import com.example.myapplication.camera.MeasurementMethod
import com.example.myapplication.camera.MultiCameraManager
import com.example.myapplication.data.entity.MeasurementResultEntity
import com.example.myapplication.data.MeasurementRepository
import com.example.myapplication.sensors.SensorInfo
import com.example.myapplication.utils.PermissionManager
import kotlinx.coroutines.launch

/**
 * ViewModel robusto y limpio para la gestión de mediciones.
 * Reescribe aquí la lógica de tu ViewModel, asegurando que uses solo los modelos y utilidades centralizados.
 * Agrega tus funciones y lógica específica según necesidad.
 */
class MeasurementViewModel(
    private val repository: MeasurementRepository,
    private val cameraManager: MultiCameraManager,
    // Agrega aquí otros managers o dependencias necesarias
) : ViewModel() {
    // Ejemplo de función para obtener resultados
    fun getAllResults() = repository.getAllResults()

    // Ejemplo de función para insertar un resultado
    fun insertResult(result: MeasurementResultEntity) {
        viewModelScope.launch {
            repository.insertResult(result)
        }
    }

    // Ejemplo de acceso correcto a las constantes de permisos:
    val requiredPermissions = PermissionManager.REQUIRED_PERMISSIONS
    val cameraPermissions = PermissionManager.CAMERA_PERMISSIONS
    val locationPermissions = PermissionManager.LOCATION_PERMISSIONS
    val storagePermissions = PermissionManager.STORAGE_PERMISSIONS

    // Agrega aquí la lógica de permisos usando PermissionManager y las constantes centralizadas
    // ...

    // Agrega aquí la lógica de sensores, cámaras, etc. usando solo dependencias limpias
    // ...
} 