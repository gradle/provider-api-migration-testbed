package test.project.build

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.language.jvm.tasks.ProcessResources

@CompileStatic
class TestGroovyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.tasks.withType(ProcessResources).configureEach { ProcessResources it ->
            it.destinationDir = project.layout.buildDirectory.dir("new-resources").get().asFile
        }

        project.tasks.withType(GroovyCompile).configureEach { GroovyCompile it ->
            it.options.compilerArgs << "-proc:none"
        }
    }
}
