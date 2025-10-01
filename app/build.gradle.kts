// File: app/build.gradle.kts
// Module: HomeoGO
// Purpose: Gradle build script for Android app module (dependencies, build config, BuildConfig fields)
// Created: 20.sep.2025 17:35
// ver. 1.0
// ↑ faila augšā (ārpus android { ... })

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {

    // obligāti!
    namespace = "lv.mariozo.homeogo"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "lv.mariozo.homeogo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // ❶ Gradle project/user properties (~/.gradle/gradle.properties vai projekta gradle.properties)
        val pKey = providers.gradleProperty("AZURE_SPEECH_KEY")
        val pRegion = providers.gradleProperty("AZURE_SPEECH_REGION")

        // ❷ Environment variables (OS līmenī)
        val eKey = providers.environmentVariable("AZURE_SPEECH_KEY")
        val eRegion = providers.environmentVariable("AZURE_SPEECH_REGION")

        // ❸ local.properties (projekta saknē; parasti Git ignorē)
        val localProps = Properties().apply {
            val f = File(rootDir, "local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        val lKey = localProps.getProperty("AZURE_SPEECH_KEY") ?: ""
        val lRegion = localProps.getProperty("AZURE_SPEECH_REGION") ?: ""

        // prioritāte: gradle.properties → ENV → local.properties → ""
        val azureKey = pKey.orElse(eKey).orElse(lKey).orElse("").get()
        val azureRegion = pRegion.orElse(eRegion).orElse(lRegion).orElse("northeurope").get()

        buildConfigField("String", "AZURE_SPEECH_KEY", "\"$azureKey\"")
        buildConfigField("String", "AZURE_SPEECH_REGION", "\"$azureRegion\"")
    }

    kotlin {
        jvmToolchain(17)
    }

    dependencies {

        implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.46.0")
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.lifecycle.viewmodel.compose) // Added this line
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material3) // For Compose Material 3
        implementation(libs.androidx.compose.material.icons.core) // Added for icons
        implementation(libs.androidx.compose.material.icons.extended) // Added for icons
        implementation(libs.material) // Added for XML Material themes
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)
        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
        implementation(libs.androidx.core.splashscreen)
        implementation(libs.okhttp3)
        implementation(libs.exoplayer)
        // implementation(libs.material3) // Removed this duplicate
        // implementation(libs.lifecycle.runtime.compose) // Removed this line, as specific version below is used
        implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
        implementation(libs.androidx.customview.poolingcontainer)


    }
}
