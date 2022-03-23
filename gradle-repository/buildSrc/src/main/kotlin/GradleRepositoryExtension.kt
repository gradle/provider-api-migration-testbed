import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

interface GradleRepositoryExtension {
    val gradleVersion: Property<String>
    val repoLocation: DirectoryProperty
}
