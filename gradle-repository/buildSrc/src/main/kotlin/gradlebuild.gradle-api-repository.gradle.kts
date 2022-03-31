plugins {
    base
    `maven-publish`
}

val gradleApiRepository = extensions.create<GradleRepositoryExtension>("gradleApiRepository")

gradleApiRepository.apply {
    gradleVersion.convention(
        providers.gradleProperty("gradleApiVersion")
    )
    repoLocation.convention(
        layout.buildDirectory.dir("repo")
    )
}

repositories {
    listOf("distributions", "distributions-snapshots").forEach { distUrl ->
        ivy {
            name = "Gradle distributions"
            url = uri("https://services.gradle.org")
            patternLayout {
                artifact("/${distUrl}/[module]-[revision]-bin(.[ext])")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("gradle-dist", "gradle")
            }
        }
    }
}

val artifactType = Attribute.of("artifactType", String::class.java)
val runtimeAttribute = objects.named<Usage>(Usage.JAVA_RUNTIME)
val gradleLibsDirArtifactType = "gradle-libs-dir"

val gradleDistribution by configurations.creating
val gradleJarsConfiguration by configurations.creating {
    extendsFrom(gradleDistribution)
    attributes.attribute(artifactType, gradleLibsDirArtifactType)
}

dependencies {
    gradleDistribution(gradleApiRepository.gradleVersion.map { "gradle-dist:gradle:${it}@zip" })

    registerTransform(ExtractJarsFromZip::class) {
        from.attribute(artifactType, "zip")
        to.attribute(artifactType, gradleLibsDirArtifactType)
    }
}

val createShadedGradleApiJar by tasks.registering(Jar::class) {
    from(gradleJarsConfiguration.asFileTree.elements
        .map { jars -> jars
            .filter { it.asFile.name.startsWith("gradle-") }
            .map { zipTree(it) }
        }
    )
    destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
    archiveFileName.set(gradleApiRepository.gradleVersion.map { "gradle-api-${it}.jar" })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

afterEvaluate {

    // We need to configure publishing in afterEvaluate,
    // since the repository URL and the MavenPublication.version aren't providers yet.

    publishing {
        repositories {
            maven {
                name = "localMaven"
                url = uri(gradleApiRepository.repoLocation)
            }
        }
        publishing.publications.create<MavenPublication>("gradleApi") {
            groupId = "org.gradle"
            artifactId = "gradle-api"
            version = gradleApiRepository.gradleVersion.get()
            artifact(createShadedGradleApiJar)
        }
    }
}
