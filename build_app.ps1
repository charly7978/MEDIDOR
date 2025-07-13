# Script para compilar la aplicación

# Configurar JAVA_HOME correctamente
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
Write-Host "JAVA_HOME configurado a: $env:JAVA_HOME"

# Limpiar el proyecto
Write-Host "Limpiando el proyecto..."
./gradlew clean

# Compilar el proyecto
Write-Host "Compilando el proyecto..."
./gradlew build

# Verificar si la compilación fue exitosa
if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilación exitosa!"
    
    # Generar APK de debug
    Write-Host "Generando APK de debug..."
    ./gradlew assembleDebug
    
    if ($LASTEXITCODE -eq 0) {
        $apkPath = "app/build/outputs/apk/debug/app-debug.apk"
        if (Test-Path $apkPath) {
            Write-Host "APK generado exitosamente en: $apkPath"
        } else {
            Write-Host "No se encontró el APK en la ruta esperada."
        }
    } else {
        Write-Host "Error al generar el APK."
    }
} else {
    Write-Host "Error en la compilación. Revisa los mensajes de error."
}
