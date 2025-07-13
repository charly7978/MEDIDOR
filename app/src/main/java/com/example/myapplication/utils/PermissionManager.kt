package com.example.myapplication.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Administrador de permisos para simplificar la solicitud y verificación de permisos en la aplicación.
 */
class PermissionManager(private val context: Context) {

    /**
     * Verifica si se ha concedido un permiso específico.
     *
     * @param permission Permiso a verificar (ej: Manifest.permission.CAMERA)
     * @return true si el permiso está concedido, false en caso contrario
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica si se han concedido todos los permisos en la lista.
     *
     * @param permissions Lista de permisos a verificar
     * @return true si todos los permisos están concedidos, false en caso contrario
     */
    fun areAllPermissionsGranted(permissions: List<String>): Boolean {
        return permissions.all { isPermissionGranted(it) }
    }

    /**
     * Verifica si se debe mostrar la explicación de por qué se necesita un permiso.
     *
     * @param permission Permiso a verificar
     * @return true si se debe mostrar la explicación, false en caso contrario
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return (activity as? FragmentActivity)?.shouldShowRequestPermissionRationale(permission) ?: false
    }

    /**
     * Verifica si se debe mostrar la explicación para cualquiera de los permisos en la lista.
     *
     * @param activity Actividad desde la que se solicita el permiso
     * @param permissions Lista de permisos a verificar
     * @return true si se debe mostrar la explicación para al menos un permiso, false en caso contrario
     */
    fun shouldShowAnyRequestPermissionRationale(activity: Activity, permissions: List<String>): Boolean {
        return permissions.any { shouldShowRequestPermissionRationale(activity, it) }
    }

    companion object {
        // Lista de permisos necesarios para la aplicación
        val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        // Permisos agrupados por funcionalidad
        val CAMERA_PERMISSIONS = listOf(Manifest.permission.CAMERA)
        val LOCATION_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val STORAGE_PERMISSIONS = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

/**
 * Hook de Composable para manejar la solicitud de permisos.
 *
 * @param permissions Lista de permisos a solicitar
 * @param onPermissionsGranted Callback que se ejecuta cuando se conceden todos los permisos
 * @param onPermissionsDenied Callback opcional que se ejecuta cuando se deniegan algunos permisos
 * @param showRationale Mensaje opcional que se muestra cuando se debe mostrar la explicación de los permisos
 * @param showPermanentlyDenied Mensaje opcional que se muestra cuando los permisos están denegados permanentemente
 * @param showSettingsButton Indica si se debe mostrar un botón para abrir la configuración de la aplicación
 * @param onShowRationale Callback opcional que se llama cuando se debe mostrar la explicación de los permisos
 * @param onShowSettings Callback opcional que se llama cuando se debe abrir la configuración de la aplicación
 */
@Composable
fun RequestPermissions(
    permissions: List<String> = PermissionManager.REQUIRED_PERMISSIONS,
    onPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: (List<String>) -> Unit = {},
    showRationale: String = "Esta función requiere permisos adicionales para funcionar correctamente.",
    showPermanentlyDenied: String = "Algunos permisos están denegados permanentemente. Por favor, actívelos manualmente en la configuración de la aplicación.",
    showSettingsButton: Boolean = true,
    onShowRationale: (String, () -> Unit) -> Unit = { _, _ -> },
    onShowSettings: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }
    var rationaleMessage by remember { mutableStateOf("") }
    var permissionsToRequest by remember { mutableStateOf(emptyList<String>()) }
    var shouldCheckPermissions by remember { mutableStateOf(true) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val deniedPermissions = permissionsResult.filter { !it.value }.keys.toList()
        
        if (deniedPermissions.isEmpty()) {
            // Todos los permisos concedidos
            onPermissionsGranted()
        } else {
            // Algunos permisos denegados
            val permanentlyDenied = deniedPermissions.any { permission ->
                !(context as? Activity)?.shouldShowRequestPermissionRationale(permission) ?: false
            }
            
            if (permanentlyDenied) {
                // Al menos un permiso está denegado permanentemente
                showPermanentlyDeniedDialog = true
            } else {
                // Mostrar explicación y volver a solicitar permisos
                rationaleMessage = showRationale
                showRationaleDialog = true
                permissionsToRequest = deniedPermissions
            }
            
            onPermissionsDenied(deniedPermissions)
        }
        
        shouldCheckPermissions = false
    }

    // Verificar permisos al inicio o cuando cambia shouldCheckPermissions
    SideEffect {
        if (shouldCheckPermissions) {
            val activity = context as? Activity ?: return@SideEffect
            
            if (permissionManager.areAllPermissionsGranted(permissions)) {
                onPermissionsGranted()
            } else {
                // Verificar si hay que mostrar explicación
                if (permissionManager.shouldShowAnyRequestPermissionRationale(activity, permissions)) {
                    rationaleMessage = showRationale
                    showRationaleDialog = true
                    permissionsToRequest = permissions
                } else {
                    // Solicitar permisos directamente
                    requestPermissionLauncher.launch(permissions.toTypedArray())
                }
            }
            
            shouldCheckPermissions = false
        }
    }

    // Diálogo de explicación de permisos
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Permisos requeridos") },
            text = { Text(rationaleMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRationaleDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de permisos denegados permanentemente
    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentlyDeniedDialog = false },
            title = { Text("Permisos requeridos") },
            text = { Text(showPermanentlyDenied) },
            confirmButton = {
                if (showSettingsButton) {
                    Button(
                        onClick = {
                            showPermanentlyDeniedDialog = false
                            onShowSettings("Por favor, active los permisos necesarios en la configuración de la aplicación.")
                        }
                    ) {
                        Text("Abrir configuración")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermanentlyDeniedDialog = false }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

/**
 * Hook de Composable para verificar un solo permiso.
 *
 * @param permission Permiso a verificar
 * @param onPermissionGranted Callback que se ejecuta cuando se concede el permiso
 * @param onPermissionDenied Callback opcional que se ejecuta cuando se deniega el permiso
 */
@Composable
fun RequestSinglePermission(
    permission: String,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    var showRationale by remember { mutableStateOf(false) }
    var showPermanentlyDenied by remember { mutableStateOf(false) }
    var shouldCheckPermission by remember { mutableStateOf(true) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            val activity = context as? Activity
            if (activity != null && !permissionManager.shouldShowRequestPermissionRationale(activity, permission)) {
                showPermanentlyDenied = true
            } else {
                onPermissionDenied?.invoke()
            }
        }
        shouldCheckPermission = false
    }

    // Verificar permiso al inicio o cuando cambia shouldCheckPermission
    SideEffect {
        if (shouldCheckPermission) {
            if (permissionManager.isPermissionGranted(permission)) {
                onPermissionGranted()
            } else {
                val activity = context as? Activity
                if (activity != null && permissionManager.shouldShowRequestPermissionRationale(activity, permission)) {
                    showRationale = true
                } else {
                    requestPermissionLauncher.launch(permission)
                }
            }
            shouldCheckPermission = false
        }
    }

    // Diálogo de explicación
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permiso requerido") },
            text = { Text("Esta función requiere el permiso de ${getPermissionName(permission)} para funcionar correctamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRationale = false
                        requestPermissionLauncher.launch(permission)
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRationale = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de permiso denegado permanentemente
    if (showPermanentlyDenied) {
        AlertDialog(
            onDismissRequest = { showPermanentlyDenied = false },
            title = { Text("Permiso requerido") },
            text = { Text("Has denegado el permiso de ${getPermissionName(permission)} permanentemente. Por favor, actívalo manualmente en la configuración de la aplicación.") },
            confirmButton = {
                Button(
                    onClick = {
                        showPermanentlyDenied = false
                        // Aquí podrías abrir la configuración de la aplicación
                    }
                ) {
                    Text("Abrir configuración")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermanentlyDenied = false }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

/**
 * Obtiene un nombre legible para un permiso.
 */
private fun getPermissionName(permission: String): String {
    return when (permission) {
        Manifest.permission.CAMERA -> "cámara"
        Manifest.permission.ACCESS_FINE_LOCATION -> "ubicación precisa"
        Manifest.permission.ACCESS_COARSE_LOCATION -> "ubicación aproximada"
        Manifest.permission.READ_EXTERNAL_STORAGE -> "lectura de almacenamiento"
        Manifest.permission.WRITE_EXTERNAL_STORAGE -> "escritura en almacenamiento"
        Manifest.permission.RECORD_AUDIO -> "grabación de audio"
        else -> permission.substringAfterLast('.').lowercase()
    }
}
