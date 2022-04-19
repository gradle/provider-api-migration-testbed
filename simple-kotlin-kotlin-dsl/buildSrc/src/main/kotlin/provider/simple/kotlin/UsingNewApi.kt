package provider.simple.kotlin

import org.gradle.api.Task

class UsingNewApi {
    fun Task.useNewApi() {
        doNotTrackState("Introduced in Gradle 7.3")
    }
}
