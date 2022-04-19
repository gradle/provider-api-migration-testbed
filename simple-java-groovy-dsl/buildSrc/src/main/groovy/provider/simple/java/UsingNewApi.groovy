package provider.simple.java;

import groovy.transform.CompileStatic;
import org.gradle.api.Task;

@CompileStatic
class UsingNewApi {
    static enabledIncompatibleMethod(Task task) {
        task.doNotTrackState("Introduced in Gradle 7.3")
    }
}
