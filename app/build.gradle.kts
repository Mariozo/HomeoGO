// File: app/build.gradle.kts
// Project: HomeoGO
// Created: 03.okt.2025 12:20 (Rīga)
// ver. 1.7
// Purpose: App module Gradle build script. Lasa Azure atslēgas no local.properties/ENV,
//          ģenerē BuildConfig laukus, pieslēdz Compose, Lifecycle-Compose, Material/AppCompat,
//          Azure Speech SDK, poolingcontainer un desugaring. SDK = 36/36, minSdk = 24.
// Comments:
//  - Keys are read from local.properties (AZURE_SPEECH_KEY/REGION) or environment variables.
//  - If missing, build still succeeds but logs a warning; runtime STT/TTS will fail.
//  - Next steps will request RECORD_AUDIO permission at runtime in MainActivity.

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        // STT valoda (default: latviešu). Vajadzības gadījumā vari nomainīt uz "en-US".
        buildConfigField("String", "STT_LANGUAGE", "\"lv-LV\"")
        // Ja true un darbojas emulatorā → izmantos sistēmas SpeechRecognizer STT
        buildConfigField("boolean", "USE_SYSTEM_STT_ON_EMULATOR", "true")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13" // paired with Kotlin 1.9.23
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Desugaring for Java 8+ APIs on minSdk < 26
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

    // Lifecycle ⇄ Compose bridges
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // XML themes
    implementation(libs.material)
    implementation(libs.androidx.appcompat)

    // Azure Speech SDK (STT + TTS)
    implementation(libs.androidx.azure.speech)

    // Compose Preview fix
    implementation(libs.androidx.customview.poolingcontainer)

    // Core library desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Unit tests
    testImplementation(libs.junit)

    // Instrumentation tests
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
