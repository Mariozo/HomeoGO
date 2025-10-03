// File: app/build.gradle.kts
// Project: HomeoGO
// Created: 03.okt.2025 11:35 (Rīga)
// ver. 1.6
// Purpose: App module Gradle build script. Ensures BuildConfig generation (with Azure fields),
//          aligns Compose/Kotlin, adds lifecycle-compose, Material/AppCompat, Azure Speech SDK,
//          poolingcontainer and desugaring. SDK = 36/36, minSdk = 24.
// Comments:
//  - buildFeatures.buildConfig = true → garantē, ka BuildConfig tiek ģenerēts (AGP 8+).
//  - defaultConfig.buildConfigField(..) pievieno AZURE_SPEECH_KEY/REGION.
//  - Exclude com.azure (Java SDK family), lai izvairītos no MethodHandle kļūdām.

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

        // Replace with secure values or inject via CI; placeholders keep compilation unblocked.
        buildConfigField("String", "AZURE_SPEECH_KEY", "\"<your_key_here>\"")
        buildConfigField("String", "AZURE_SPEECH_REGION", "\"westeurope\"")
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
        buildConfig = true   // ← ensure BuildConfig is generated
    }

    composeOptions {
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
}
