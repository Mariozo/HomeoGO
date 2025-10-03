// File: build.gradle.kts
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 11:10 (Rīga)
// ver. 1.3
// Purpose: Root Gradle script. Declares plugins via version catalog aliases.
// Comments:
//  - Uses [plugins] section from gradle/libs.versions.toml.
//  - No cieti ieliktas versijas; IDE vairs nerādīs “update recommended”.
//  - Provides clean task.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
