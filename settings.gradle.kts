pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// Enforce Gradle version
gradle.rootProject {
    val expectedVersion = "8.13" // Set the locked Gradle version
    if (gradle.gradleVersion != expectedVersion) {
        throw GradleException("Expected Gradle version $expectedVersion but found ${gradle.gradleVersion}. Please use the Gradle wrapper (gradlew) instead of your local Gradle installation.")
    }
}

rootProject.name = "Local Connect"
include(":app")
