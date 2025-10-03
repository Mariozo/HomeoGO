// File: app/build.gradle.kts
// Project: HomeoGO
// Created: 03.okt.2025 08:00 (Rīga)
// ver. 1.1
// Purpose: App module Gradle build script with Compose + Azure Speech SDK.
// Comments:
//  - Added implementation(libs.androidx.azure.speech).
//  - Make sure minSdk >= 24, compileSdk = 35 as in repo.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "lv.mariozo.homeogo"
    compileSdk = 36

    defaultConfig {
        applicationId = "lv.mariozo.homeogo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Material Components for XML themes (e.g., splash screen)
    implementation(libs.material)

    // Azure Speech SDK (STT + TTS)
    implementation(libs.androidx.azure.speech)

    // Core library desugaring
    coreLibraryDesugaring(libs.desugarJdkLibs)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
