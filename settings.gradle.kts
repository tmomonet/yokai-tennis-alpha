pluginManagement {
    repositories {
        // Local stub repo provides the AGP 9.0.0 plugin marker POM so that
        // :core:test can be built without resolving the marker from the network.
        // The actual AGP 9.0.0 jar is already in the Gradle files cache.
        maven { url = uri("local-plugin-repo") }
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        // Local stub repo for artifacts not downloadable due to environment constraints.
        maven { url = uri("local-plugin-repo") }
        mavenCentral()
        google()
    }
}

rootProject.name = "tennis"
include("core", "desktop", "android")
