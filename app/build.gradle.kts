// File: app/build.gradle.kts
// Module: HomeoGO
// Purpose: Gradle build script for Android app module (dependencies, build config, BuildConfig fields)
// Created: 20.sep.2025 17:35
// ver. 1.0

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "lv.mariozo.homeogo"
    compileSdk = 36

    defaultConfig {
        applicationId = "lv.mariozo.homeogo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // ✅ ABI filteri – tie paliek iekš ndk
        ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        // ✅ BuildConfig lauki – ārpus ndk!
        val azureKey = providers.gradleProperty("AZURE_SPEECH_KEY").orElse("").get()
        val azureRegion =
            providers.gradleProperty("AZURE_SPEECH_REGION").orElse("northeurope").get()

        buildConfigField("String", "AZURE_SPEECH_KEY", "\"$azureKey\"")
        buildConfigField("String", "AZURE_SPEECH_REGION", "\"$azureRegion\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildFeatures {
                buildConfig = true   // ← obligāti ieslēdzam
                compose = true
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        kotlinOptions {
            jvmTarget = "17"
        }

        buildFeatures {
            compose = true
        }

        composeOptions {
            kotlinCompilerExtensionVersion = "1.5.14"
        }

        packaging {
            resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        }
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
    }
}