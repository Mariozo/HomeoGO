// File: settings.gradle.kts
// Project: HomeoGO
// Created: 15.okt.2025 (Rīga)
// ver. 1.1 (FIX - Added plugin repositories)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HomeoGO"
include(":app")
