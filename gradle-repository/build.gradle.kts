import java.nio.file.Files
import java.util.zip.ZipInputStream

plugins {
    `base`
    `maven-publish`
}
interface GradleRepositoryExtension {
    val gradleVersion: Property<String>
}

val gradleExtension = extensions.create<GradleRepositoryExtension>("gradleRepository")

gradleExtension.gradleVersion.convention("7.4.1")

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

val ARTIFACT_TYPE = Attribute.of("artifactType", String::class.java)
val RUNTIME_ATTRIBUTE = objects.named<Usage>(Usage.JAVA_RUNTIME)

val gradleDistribution by configurations.creating
val gradleClasspath by configurations.creating {
    extendsFrom(gradleDistribution)
    attributes.attribute(ARTIFACT_TYPE, "gradle-classpath")
}
val gradleJarsConfiguration by configurations.creating {
    extendsFrom(gradleDistribution)
    attributes.attribute(ARTIFACT_TYPE, "gradle-libs-dir")
}

dependencies {
    gradleDistribution(gradleExtension.gradleVersion.map { "gradle-dist:gradle:${it}@zip" })

    registerTransform(ExplodeZipAndFindJars::class) {
        from.attribute(ARTIFACT_TYPE, "zip")
        to.attribute(ARTIFACT_TYPE, "gradle-libs-dir")
    }
}

val resolveJars by tasks.registering(Sync::class) {
    from(gradleJarsConfiguration) {
        into("third-party")
        exclude("gradle-*")
    }
    from(gradleJarsConfiguration) {
        into("gradle-jars")
        include("gradle-*")
    }
    into(project.buildDir.resolve("libs"))
}

val repoLocation = project.buildDir.resolve("repo")

val createGradleArtifactList by tasks.registering(CreateGradleArtifactList::class) {
    gradleJars from gradleJarsConfiguration
    gradleVersion from gradleExtension.gradleVersion
    artifactList.set(layout.buildDirectory.file(gradleExtension.gradleVersion.map { "gradle-$it-artifacts.txt" }))
}

publishing {
    repositories {
        maven {
            name = "localMaven"
            url = uri(repoLocation)
        }
    }
}

val gradleArtifactList = providers.fileContents(createGradleArtifactList.flatMap { it.artifactList })

val gradleArtifacts = gradleArtifactList.asText.map {
    it.lines().filter(String::isNotBlank)
}

project.afterEvaluate {
    if (gradleArtifacts.isPresent) {
        val gradleVersion = gradleExtension.gradleVersion.get()
        gradleArtifacts.get().forEach { artifactName ->
            publishing.publications.create<MavenPublication>(artifactName) {
                groupId = "org.gradle"
                artifactId = artifactName
                version = gradleExtension.gradleVersion.get()
                artifact(resolveJars.map { it.destinationDir.resolve("gradle-jars/$artifactName-$gradleVersion.jar") })
            }
        }
        publishing.publications.create<MavenPublication>("gradle-api") {
            groupId = "org.gradle"
            artifactId = "gradle-api"
            version = gradleExtension.gradleVersion.get()
            pom {
                packaging = "pom"
                withXml {
                    val dependencies = asNode().appendNode("dependencies")
                    gradleArtifacts.get().forEach { artifactName ->
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

infix fun ConfigurableFileCollection.from(files: Any) {
    from(files)
}

infix fun <T> Property<T>.from(value: T) {
    set(value)
}

infix fun <T> Property<T>.from(value: Provider<T>) {
    set(value)
}
@DisableCachingByDefault(because = "Not worth caching")
abstract class CreateGradleArtifactList : DefaultTask() {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputFiles
    abstract val gradleJars: ConfigurableFileCollection

    @get:Input
    abstract val gradleVersion: Property<String>

    @get:OutputFile
    abstract val artifactList: RegularFileProperty

    @TaskAction
    fun createArtifactList() {
        val versionPostfix = "-${gradleVersion.get()}.jar"
        println(versionPostfix)
        val artifacts = gradleJars.asFileTree.files
            .map { it.name }
            .filter { it.startsWith("gradle-") && it.endsWith(versionPostfix) }
            .map { it.substring(0, it.length - versionPostfix.length) }
        artifactList.get().asFile.writeText(artifacts.joinToString("\n"))
    }
}
