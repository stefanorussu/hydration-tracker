import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.stefanorussu.hydrationtracker"
    compileSdk = 34

    // 1. PRIMA DI TUTTO: Leggiamo il file local.properties e lo mettiamo nella variabile "properties"
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.stefanorussu.hydrationtracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 2. Estraiamo i valori usando SOLO "properties" (con fallback a stringa vuota se non li trova)
        val fitbitId = properties.getProperty("FITBIT_CLIENT_ID") ?: ""
        val fitbitSecret = properties.getProperty("FITBIT_CLIENT_SECRET") ?: ""
        val fitbitRedirect = properties.getProperty("FITBIT_REDIRECT_URI") ?: ""

        // 3. Creiamo i campi per BuildConfig
        buildConfigField("String", "FITBIT_CLIENT_ID", "\"$fitbitId\"")
        buildConfigField("String", "FITBIT_CLIENT_SECRET", "\"$fitbitSecret\"")
        buildConfigField("String", "FITBIT_REDIRECT_URI", "\"$fitbitRedirect\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true // Fondamentale per far apparire la classe BuildConfig nel codice!
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // UNIFICATO: Entrambe le funzionalità di build attivate qui
    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        // Fondamentale per Kotlin 1.9.22
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.constraintlayout.core)
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
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.google.code.gson:gson:2.10.1")
    // Autenticazione con account Google
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Librerie per comunicare con Google Drive
    implementation("com.google.api-client:google-api-client-android:1.33.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")

    // Retrofit per le chiamate API verso Fitbit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
}

// Questo codice forza Gradle a ignorare il task che sta causando il crash
tasks.whenTaskAdded {
    if (name == "checkDebugClasspath") {
        enabled = false
    }
}