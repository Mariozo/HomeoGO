// File: app/build.gradle.kts
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 08:55 (Rīga)
// ver. 1.3
// Purpose: App module Gradle build script with Compose + Azure Speech SDK.
//          Uses compileSdk/targetSdk = 36 (Android 15), excludes com.azure Java SDKs,
//          and enables core library desugaring for minSdk < 26 compatibility.
// Comments:
//  - SDK levels: compileSdk = 36, targetSdk = 36, minSdk = 24.
//  - FIX (kept): exclude(group = "com.azure") to avoid pulling azure-core (MethodHandle on <26).
//  - FIX (kept): coreLibraryDesugaring enabled; requires libs.desugar.jdk.libs alias in TOML.
//  - Compose versions aligned via BOM; compiler ext 1.5.13.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "lv.mariozo.homeogo"
    compileSdk = 36

    defaultConfig {
        applicationId = "lv.mariozo.homeogo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
    }

    composeOptions {
        // Align with Compose BOM used in version catalog
        kotlinCompilerExtensionVersion = "1.5.13"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Azure Speech SDK (STT + TTS)
    implementation(libs.androidx.azure.speech)

    // Fix Compose Preview crash (poolingcontainer)
    implementation(libs.androidx.customview.poolingcontainer)

    // Core library desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // --- patch start: app/build.gradle.kts — add XML theme deps -------------------
    dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)

        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material3)

        // XML tēmu bibliotēkas (nepieciešamas Theme.Material3.DayNight* un windowActionBar u.c. atribūtiem)
        implementation(libs.material)
        implementation(libs.androidx.appcompat)

        // Azure Speech SDK (STT + TTS)
        implementation(libs.androidx.azure.speech)

        // Fix Compose Preview crash (poolingcontainer)
        implementation(libs.androidx.customview.poolingcontainer)

        // Core library desugaring
        coreLibraryDesugaring(libs.desugar.jdk.libs)

        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
    }
// --- patch end ----------------------------------------------------------------

}
