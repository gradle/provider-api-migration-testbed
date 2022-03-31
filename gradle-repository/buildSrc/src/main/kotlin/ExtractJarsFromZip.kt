import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream

@DisableCachingByDefault(because = "Not worth caching")
abstract class ExtractJarsFromZip : TransformAction<TransformParameters.None> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val artifact: Provider<FileSystemLocation>
    override
    fun transform(outputs: TransformOutputs) {
        val gradleJars = outputs.dir("gradle-jars")
        ZipInputStream(Files.newInputStream(artifact.get().asFile.toPath())).use { zin ->
            generateSequence { zin.nextEntry }.forEach { zipEntry ->
                var shortName: String = zipEntry.name
                if (shortName.contains('/')) {
                    shortName = shortName.substring(shortName.lastIndexOf('/') + 1)
                }
                if (shortName.endsWith(".jar")) {
                    val out = File(gradleJars, shortName)
                    Files.copy(zin, out.toPath())
                    zin.closeEntry()
                }
            }
        }
    }
}
