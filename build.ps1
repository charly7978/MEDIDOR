# Script para compilar manualmente el proyecto Android

# Configuración
$JAVA_HOME = "C:\Program Files\Java\jdk-21"  # Ruta de JDK detectada
$env:JAVA_HOME = $JAVA_HOME  # Establecer la variable de entorno JAVA_HOME
$ANDROID_SDK = "$env:LOCALAPPDATA\Android\Sdk"
$BUILD_TOOLS = "$ANDROID_SDK\build-tools\34.0.0"  # Usando la versión disponible
$PLATFORM = "$ANDROID_SDK\platforms\android-34"  # Usando la versión disponible

# Directorios
$SRC_DIR = "app\src\main\java"
$RES_DIR = "app\src\main\res"
$ASSETS_DIR = "app\src\main\assets"
$GEN_DIR = "build\generated"
$OUT_DIR = "build\outputs"
$CLASSES_DIR = "$OUT_DIR\classes"
$DEX_DIR = "$OUT_DIR\dex"
$APK_DIR = "$OUT_DIR\apk"

# Crear directorios necesarios
New-Item -ItemType Directory -Force -Path $GEN_DIR, $CLASSES_DIR, $DEX_DIR, $APK_DIR | Out-Null

# 1. Generar R.java
echo "Generando R.java..."
& "$BUILD_TOOLS\aapt.exe" package -f -m -J $GEN_DIR -M "app\src\main\AndroidManifest.xml" -S $RES_DIR -I "$PLATFORM\android.jar"

if ($LASTEXITCODE -ne 0) {
    echo "Error al generar R.java"
    exit 1
}

# 2. Compilar código Java
echo "Compilando código Java..."
$JAVA_FILES = Get-ChildItem -Path $SRC_DIR, $GEN_DIR -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
& "$JAVA_HOME\bin\javac.exe" -d $CLASSES_DIR -cp "$PLATFORM\android.jar" -source 1.8 -target 1.8 $JAVA_FILES

if ($LASTEXITCODE -ne 0) {
    echo "Error al compilar el código Java"
    exit 1
}

# 3. Crear archivo DEX
echo "Creando archivo DEX..."
$CLASS_FILES = Get-ChildItem -Path $CLASSES_DIR -Recurse -Filter *.class | Select-Object -ExpandProperty FullName
& "$BUILD_TOOLS\d8.bat" --output $DEX_DIR $CLASS_FILES --lib "$PLATFORM\android.jar"

if ($LASTEXITCODE -ne 0) {
    echo "Error al crear el archivo DEX"
    exit 1
}

# 4. Crear APK sin firmar
echo "Creando APK sin firmar..."
& "$BUILD_TOOLS\aapt.exe" package -f -M "app\src\main\AndroidManifest.xml" -S $RES_DIR -I "$PLATFORM\android.jar" -F "$APK_DIR\app-unsigned.apk" "$DEX_DIR"

if ($LASTEXITCODE -ne 0) {
    echo "Error al crear el APK sin firmar"
    exit 1
}

# 5. Firmar el APK (usando el keystore de depuración por defecto)
echo "Firmando APK..."
$KEYSTORE = "$env:USERPROFILE\.android\debug.keystore"
$KEYSTORE_PASS = "android"
$KEY_ALIAS = "androiddebugkey"
$KEY_PASS = "android"

& "$BUILD_TOOLS\apksigner.bat" sign --ks $KEYSTORE --ks-pass "pass:$KEYSTORE_PASS" --key-pass "pass:$KEY_PASS" --ks-key-alias $KEY_ALIAS --out "$APK_DIR\app-debug.apk" "$APK_DIR\app-unsigned.apk"

if ($LASTEXITCODE -ne 0) {
    echo "Error al firmar el APK"
    exit 1
}

echo "¡Compilación completada con éxito!"
echo "APK generado en: $APK_DIR\app-debug.apk"