// File: app/build.gradle.kts
// Project: HomeoGO
// Created: 15.okt.2025 (Rīga)
// ver. 2.6 (FIX - Align Compose Compiler with Kotlin version)

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "lv.mariozo.homeogo"
    compileSdk = 36

    // --- Load secrets from local.properties or ENV ----------------------------
    val props = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val azureKey =
        (props.getProperty("AZURE_SPEECH_KEY") ?: System.getenv("AZURE_SPEECH_KEY")).orEmpty()
    val azureRegion =
        (props.getProperty("AZURE_SPEECH_REGION") ?: System.getenv("AZURE_SPEECH_REGION")).orEmpty()
    val geminiKey =
        (props.getProperty("GEMINI_API_KEY") ?: System.getenv("GEMINI_API_KEY")).orEmpty()

    if (azureKey.isBlank() || azureRegion.isBlank()) {
        logger.warn("⚠️ Azure Speech atslēgas nav atrastas (AZURE_SPEECH_KEY / AZURE_SPEECH_REGION). STT/TTS neautentificēsies.")
    }

    defaultConfig {
        applicationId = "lv.mariozo.homeogo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Provide keys via BuildConfig (read from local.properties/ENV)
        buildConfigField("String", "AZURE_SPEECH_KEY", "\"$azureKey\"")
        buildConfigField("String", "AZURE_SPEECH_REGION", "\"$azureRegion\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")

        // STT valoda (default: latviešu). Vajadzības gadījumā vari nomainīt uz "en-US".
        buildConfigField("String", "STT_LANGUAGE", "\"lv-LV\"")

        // Ja true un darbojas emulatorā → izmantos sistēmas SpeechRecognizer STT
        buildConfigField("boolean", "USE_SYSTEM_STT_ON_EMULATOR", "true")

        // Elza AI backend API (for /elza/reply endpoint)
        val elzaBase = (props.getProperty("ELZA_API_BASE")
            ?: System.getenv("ELZA_API_BASE")
            ?: "http://10.0.2.2:5000").trim()

        val elzaPath = (props.getProperty("ELZA_API_PATH")
            ?: System.getenv("ELZA_API_PATH")
            ?: "/elza/reply").trim()

        val elzaToken = (props.getProperty("ELZA_API_TOKEN")
            ?: System.getenv("ELZA_API_TOKEN")
            ?: "").trim()

        buildConfigField("String", "ELZA_API_BASE", "\"$elzaBase\"")
        buildConfigField("String", "ELZA_API_PATH", "\"$elzaPath\"")
        buildConfigField("String", "ELZA_API_TOKEN", "\"$elzaToken\"")

    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Desugaring for Java 8+ APIs on minSdk < 26 (must live inside android {})
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

// Keep Microsoft Speech SDK only; exclude Azure Java SDK family
configurations.configureEach {
    exclude(group = "com.azure")
}

dependencies {
    // Core + lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose activity host
    implementation(libs.androidx.activity.compose)

    // Compose (BOM-managed)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // SPS-37: FIX - Replaced unresolved 'libs' reference
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // Lifecycle ⇄ Compose bridges
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // DataStore (for settings)
    // SPS-37: FIX - Replaced unresolved 'libs' reference
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // XML themes
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.androidx.appcompat)

    // Azure Speech SDK (STT + TTS)
    // FIX - Replaced unresolved 'libs' reference
    implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.39.0")

    // Compose Preview fix
    implementation(libs.androidx.customview.poolingcontainer)

    // Core library desugaring
    // FIX - Replaced unresolved 'libs' reference
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Unit tests
    testImplementation(libs.junit)

    // Instrumentation tests
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

