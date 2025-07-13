plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.22"
    id("com.google.dagger.hilt.android") version "2.51.1"
    kotlin("kapt") version "1.9.22"
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("measurementAppStoreFile") ?: "")
            storePassword = project.findProperty("measurementAppStorePassword") as String? ?: ""
            keyAlias = project.findProperty("measurementAppKeyAlias") as String? ?: ""
            keyPassword = project.findProperty("measurementAppKeyPassword") as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    buildToolsVersion = "34.0.0"
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/gradle/incremental.annotation.processors"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0")

    // Hilt para inyección de dependencias
    val hiltVersion = "2.51.1"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.1")
    
    // ViewModel y Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Room para base de datos local
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion") // Soporte para corrutinas
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // Para usar el soporte de corrutinas con Room
    implementation("androidx.room:room-ktx:$roomVersion")
    
    // Gson para conversión de tipos
    implementation("com.google.code.gson:gson:2.10.1")

    // CameraX para múltiples cámaras y visión avanzada
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-video:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("androidx.camera:camera-extensions:1.4.2")

    // ARCore para medición 3D real
    implementation("com.google.ar:core:1.50.0")

    // TensorFlow Lite para IA
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")

    // Sensores y ubicación
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ML Kit para detección de objetos
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:object-detection-custom:17.0.2")

    // Matemáticas avanzadas
    implementation("org.apache.commons:commons-math3:3.6.1")

    // Permisos dinámicos
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.6.3")
}

// Configuración de Hilt
kapt {
    correctErrorTypes = true
}
