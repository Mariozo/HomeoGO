// File: app/build.gradle.kts
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 09:50 (Rīga)
// ver. 1.4
// Purpose: App module Gradle build script with Compose, Lifecycle-Compose bridges,
//          Azure Speech SDK, Material/AppCompat XML themes, poolingcontainer (Preview),
//          and desugaring. SDK = 36/36; Kotlin/Compose compiler aligned.
// Comments:
//  - Added lifecycle-runtime-compose & lifecycle-viewmodel-compose to fix unresolved imports.
//  - Ensured material + appcompat present for Theme.Material3.DayNight.* parents/attrs.
//  - Exclude com.azure (Java SDK family) to avoid MethodHandle issues on <26.
//  - Compose compiler ext 1.5.13 (paired with Kotlin 1.9.23 via root plugins).

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
        // Aligned with Kotlin 1.9.23 (see Compose-Kotlin compatibility map)
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
    // Core + lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Activity Compose host
    implementation(libs.androidx.activity.compose)

    // Compose (BOM-managed)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Lifecycle ⇄ Compose bridges (collectAsStateWithLifecycle, viewModel())
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // XML theme libs for Theme.Material3.DayNight.* and attrs like windowActionBar
    implementation(libs.material)
    implementation(libs.androidx.appcompat)

    // Azure Speech SDK (STT + TTS)
    implementation(libs.androidx.azure.speech)

    // Preview poolingcontainer fix
    implementation(libs.androidx.customview.poolingcontainer)

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
