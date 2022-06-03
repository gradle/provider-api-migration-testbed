package test.project.build;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.testing.base.TestingExtension;

public class TestJavaPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java-library");

        project.getTasks().withType(JavaCompile.class).configureEach(compile -> {
            compile.setSourceCompatibility("11");
            compile.setTargetCompatibility("11");
            compile.getOptions().getCompilerArgs().add("-proc:none");
            compile.doLast(new Action<Task>() {
                @Override
                public void execute(Task task) {
                    System.out.printf("Hello from Java plugin, classpath length: %d%n", compile.getClasspath().getFiles().size());
                }
            });
        });
        project.getTasks().withType(ProcessResources.class).configureEach(processResources -> {
            processResources.setDestinationDir(project.getLayout().getBuildDirectory().dir("new-resources").get().getAsFile());
        });

        project.getDependencies().constraints(constraints -> {
            constraints.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,"org.apache.commons:commons-text:1.9");
        });

        project.getExtensions().configure(TestingExtension.class, testing -> {
            ((JvmTestSuite) testing.getSuites().getByName("test")).useJUnitJupiter("5.8.1");
        });
    }
}
