plugins {
    base
    `maven-publish`
}

val gradleApiRepository = extensions.create<GradleRepositoryExtension>("gradleApiRepository")

gradleApiRepository.apply {
    gradleVersion.convention(
        providers.gradleProperty("gradleApiVersion").orElse("7.4.1")
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

val gradleDistribution by configurations.creating
val gradleClasspath by configurations.creating {
    extendsFrom(gradleDistribution)
    attributes.attribute(artifactType, "gradle-classpath")
}
val gradleJarsConfiguration by configurations.creating {
    extendsFrom(gradleDistribution)
    attributes.attribute(artifactType, "gradle-libs-dir")
}
val gradleArtifactListConfiguration by configurations.creating {
    extendsFrom(gradleDistribution)
    attributes.attribute(artifactType, "gradle-artifact-list")
}

dependencies {
    gradleDistribution(gradleApiRepository.gradleVersion.map { "gradle-dist:gradle:${it}@zip" })

    registerTransform(ExplodeZipAndFindJars::class) {
        from.attribute(artifactType, "zip")
        to.attribute(artifactType, "gradle-libs-dir")
    }
    registerTransform(CreateGradleArtifactList::class) {
        parameters {
            gradleVersion.set(gradleApiRepository.gradleVersion)
        }
        from.attribute(artifactType, "gradle-libs-dir")
        to.attribute(artifactType, "gradle-artifact-list")
    }
}

val thirdPartyJarsPath = "third-party"
val gradleJarsPath = "gradle-jars"

val resolveJars by tasks.registering(Sync::class) {
    from(gradleJarsConfiguration) {
        into(thirdPartyJarsPath)
        exclude("gradle-*")
    }
    from(gradleJarsConfiguration) {
        into(gradleJarsPath)
        include("gradle-*")
    }
    into(project.buildDir.resolve("libs"))
}

afterEvaluate {
    val gradleApiJarGroupId = "org.gradle"

    publishing {
        repositories {
            maven {
                name = "localMaven"
                url = uri(gradleApiRepository.repoLocation)
            }
        }
    }

    if (gradle.startParameter.taskNames.any { it.contains("publish") }) {
        val artifactList = gradleArtifactListConfiguration.singleFile
        val gradleArtifacts = artifactList.readLines().filter(String::isNotBlank)
        val gradleVersion = gradleApiRepository.gradleVersion.get()
        gradleArtifacts.forEach { artifactName ->
            publishing.publications.create<MavenPublication>(artifactName) {
                groupId = gradleApiJarGroupId
                artifactId = artifactName
                version = gradleApiRepository.gradleVersion.get()
                artifact(resolveJars.map { it.destinationDir.resolve("$gradleJarsPath/$artifactName-$gradleVersion.jar") })
            }
        }
        publishing.publications.create<MavenPublication>("gradle-api") {
            groupId = gradleApiJarGroupId
            artifactId = "gradle-api"
            version = gradleApiRepository.gradleVersion.get()
            pom {
                packaging = "pom"
                withXml {
                    val dependencies = asNode().appendNode("dependencies")
                    gradleArtifacts.forEach { artifactName ->
                        dependencies.appendNode("dependency").apply {
                            appendNode("groupId", "org.gradle")
                            appendNode("artifactId", artifactName)
                            appendNode("version", gradleVersion)
                        }
                    }
                }
            }
        }
    }
}
