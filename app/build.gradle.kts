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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AZURE_SPEECH_KEY", "\"${project.properties["AZURE_SPEECH_KEY"]}\"")
        buildConfigField("String", "AZURE_SPEECH_REGION", "\"${project.properties["AZURE_SPEECH_REGION"]}\"")
    }

    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13" // Added for Kotlin 2.0.21
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