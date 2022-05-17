import test.project.build.TestKotlinPlugin

plugins {
    kotlin("jvm")
    `java-library`
}

pluginManager.apply(TestKotlinPlugin::class)

tasks.withType<AbstractCompile>().configureEach {
    targetCompatibility = "11"
    sourceCompatibility = "11"
    doLast {
        println("Hello from Kotlin plugin ${classpath.files.size}")
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
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.8.1")
        }
    }
}