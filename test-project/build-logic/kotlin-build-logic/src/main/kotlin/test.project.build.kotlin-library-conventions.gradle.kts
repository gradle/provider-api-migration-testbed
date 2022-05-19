import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import test.project.build.TestKotlinPlugin

plugins {
    kotlin("jvm")
    `java-library`
}

pluginManager.apply(TestKotlinPlugin::class)

tasks.withType<AbstractCompile>().configureEach {
    targetCompatibility = "11"
    sourceCompatibility = "11"
    classpath += files()
    doLast {
        println("Hello from Kotlin plugin, classpath length: ${classpath.files.size}")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    constraints {
        // Define dependency versions as constraints
        implementation("org.apache.commons:commons-text:1.9")

        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
}

testing {
    suites {
        // Configure the built-in test suite
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter("5.8.1")
        }
    }
}