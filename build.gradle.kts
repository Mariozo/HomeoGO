// File: build.gradle.kts
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 09:30 (Rīga)
// ver. 1.2
// Purpose: Root Gradle script. Declares plugins with versions via version catalog.
// Comments:
//  - Uses plugin aliases defined in gradle/libs.versions.toml ([plugins] section).
//  - Removes hardcoded versions (no more “Project update recommended”).
//  - Provides clean task for convenience.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
