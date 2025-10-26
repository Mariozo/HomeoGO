// File: settings.gradle.kts
// Project: HomeoGO
// Created: 16.okt.2025 10:30 (Rīga)
// ver. 1.1 (DEBUG - Using explicit repository URLs)
// Purpose: Defines project structure and repositories. Explicit URLs used to debug network issues.

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
