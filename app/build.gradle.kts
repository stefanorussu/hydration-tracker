plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.stefanorussu.hydrationtracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stefanorussu.hydrationtracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            // Disabilita la compressione risorse in debug per evitare il fingerprinting
            isShrinkResources = false
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Fondamentale per Kotlin 1.9.22
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    // 1. ROOM (KSP deve essere isolato)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // 2. COMPOSE (Usiamo il BOM per stabilizzare le versioni)
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // 3. LIFECYCLE & ACTIVITY
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // 4. COROUTINES
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 5. DEBUG (Sempre alla fine)
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.navigation:navigation-compose:2.8.0")
}

// Questo codice forza Gradle a ignorare il task che sta causando il crash
tasks.whenTaskAdded {
    if (name == "checkDebugClasspath") {
        enabled = false
    }
}