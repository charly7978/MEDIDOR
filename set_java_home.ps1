# Script para configurar JAVA_HOME correctamente
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
Write-Host "JAVA_HOME configurado a: $env:JAVA_HOME"

# Verificar la configuración
if (Test-Path "$env:JAVA_HOME\bin\java.exe") {
    Write-Host "Configuración correcta: java.exe encontrado en $env:JAVA_HOME\bin"
} else {
    Write-Host "ERROR: No se encontró java.exe en $env:JAVA_HOME\bin"
    Write-Host "Por favor, ajusta la ruta en este script al JDK correcto"
}