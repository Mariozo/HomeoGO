// File: build.gradle.kts
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 11:10 (Rīga)
// ver. 1.3
// Purpose: Root Gradle script. Declares plugins via version catalog aliases.
// Comments:
//  - Uses [plugins] section from gradle/libs.versions.toml.
//  - No cieti ieliktas versijas; IDE vairs nerādīs “update recommended”.
//  - Provides clean task.

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun prop(name: String, default: String = ""): String =
    (localProps.getProperty(name) ?: System.getenv(name) ?: default)

android {
    namespace = "lv.mariozo.homeogo"
    compileSdk = 34

    defaultConfig {
        applicationId = "lv.mariozo.homeogo"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // === BuildConfig konstantes, ko izmanto ElzaViewModel/SpeechRecognizerManager ===
        buildConfigField("String", "AZURE_SPEECH_KEY", "\"${prop("AZURE_SPEECH_KEY", "")}\"")
        buildConfigField(
            "String",
            "AZURE_SPEECH_REGION",
            "\"${prop("AZURE_SPEECH_REGION", "eastus")}\""
        )
        buildConfigField("String", "STT_LANGUAGE", "\"lv-LV\"")

        buildConfigField("String", "ELZA_API_BASE", "\"https://example.com\"")
        buildConfigField("String", "ELZA_API_PATH", "\"/api\"")
        buildConfigField("String", "ELZA_API_TOKEN", "\"dev-token\"")
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Sader ar Kotlin 1.9.23
        kotlinCompilerExtensionVersion = "1.5.13"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Compose BOM (nosaka Compose moduļu versijas)
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))

    // AndroidX pamatkomponentes
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.customview:customview-poolingcontainer:1.0.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Compose UI (versijas nāk no BOM)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Material XML tēmas (Theme.Material3.*)
    implementation("com.google.android.material:material:1.12.0")

    // DataStore Preferences (SettingsRepository)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Compose Material icons (Mic, VolumeOff u.c.)
    implementation("androidx.compose.material:material-icons-extended")

    // Azure Speech SDK (SpeechSynthesisCancellationDetails u.c.)
    implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.46.0")

    // (Pēc vajadzības) korutīnas Android
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
