import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

abstract class CreateGradleArtifactList : TransformAction<CreateGradleArtifactList.Parameters> {

    interface Parameters : TransformParameters {
        @get:Input
        val gradleVersion: Property<String>
    }

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val artifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val versionPostfix = "-${parameters.gradleVersion.get()}.jar"
        val artifactsNames = artifact.get().asFile.list()
            .filter { it.startsWith("gradle-") && it.endsWith(versionPostfix) }
            .map { it.substring(0, it.length - versionPostfix.length) }
            .sorted()
        val outputFile = outputs.file("gradle-artifacts.txt")
        outputFile.writeText(artifactsNames.joinToString("\n"))
    }
}
