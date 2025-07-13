plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.professional.measurement.ar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.professional.measurement.ar"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Firmar con clave de debug para testing
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
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Dependencias básicas para funcionalidad inicial
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation(libs.androidx.activity.ktx)

    // Dependencias avanzadas (comentadas temporalmente para evitar crashes)
    // CameraX para múltiples cámaras
    // implementation("androidx.camera:camera-core:1.3.1")
    // implementation("androidx.camera:camera-camera2:1.3.1")
    // implementation("androidx.camera:camera-lifecycle:1.3.1")
    // implementation("androidx.camera:camera-video:1.3.1")
    // implementation("androidx.camera:camera-view:1.3.1")
    // implementation("androidx.camera:camera-extensions:1.3.1")

    // ARCore para medición 3D real
    // implementation("com.google.ar:core:1.40.0")

    // TensorFlow Lite para IA
    // implementation("org.tensorflow:tensorflow-lite:2.14.0")
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    // implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Sensores y ubicación
    // implementation("com.google.android.gms:play-services-location:21.0.1")

    // ML Kit para detección de objetos
    // implementation("com.google.mlkit:object-detection:17.0.0")
    // implementation("com.google.mlkit:object-detection-custom:17.0.0")

    // Matemáticas avanzadas
    // implementation("org.apache.commons:commons-math3:3.6.1")

    // Permisos dinámicos
    // implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}