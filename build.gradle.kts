// File: build.gradle.kts
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 11:10 (Rīga)
// ver. 1.5 (DEBUG - Bypassing version catalog)
// Purpose: Root Gradle script. Declares plugins directly to debug sync issues.

plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
}

