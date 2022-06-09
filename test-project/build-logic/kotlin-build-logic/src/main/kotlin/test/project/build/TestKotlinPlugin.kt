package test.project.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

class TestKotlinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.withType<ProcessResources>().configureEach {
            destinationDir = project.layout.buildDirectory.dir("new-resources").get().asFile
        }

        project.tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.add("-Xmx512M")
        }
    }
}
