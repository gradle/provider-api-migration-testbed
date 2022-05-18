include("groovy-build-logic", "java-build-logic", "kotlin-build-logic")

pluginManagement {
    repositories {
        maven {
            url = uri("https://repo.grdev.net/artifactory/ext-snapshots-local")
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://repo.grdev.net/artifactory/ext-snapshots-local")
        }
    }
}