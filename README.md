# Provider API migration testbed

Gradle is planning to migrate all existing public API to use the [lazy configuration](https://docs.gradle.org/current/userguide/lazy_configuration.html) patters, aka the "Provider API."
In a large part this means changing JavaBean properties on tasks and extensions to their `Property<T>` counterparts.
Since these are all breaking changes, we need to also include mitigation strategies to prevent breaking user builds.

This repository contains a testbed to try out how this migration will work from a user's point of view.
This is a proof-of-concept demonstration to help discussion around the planned migration, not a fully fledged implementation.

In `test-project` you can find a simple Gradle project that works with an older Gradle version.
The build logic accesses some to-be-migrated JavaBean properties on the old Gradle API.
During the demonstration we will upgrade this project to use a new Gradle version that has these properties upgraded.
The project also uses the Kotlin plugin, which itself also references some of the to-be-migrated properties.

In `gradle-next` there is the patched version of Gradle created for this demonstration.
It includes examples of the kinds of changes we are planning to make across Gradle's public APIs as part of the provider API migration:

* In `AbstractCompile`
  * `sourceCompatibility` & `targetCompatibility` was converted from `String` to `Property<String>`,
  * `classpath` was converted from `FileCollection` to `ConfigurableFileCollection`,
* In `CompileOptions`
  * `compileArgs` was converted from `List<String>` to `ListProperty<String>`,
* In `Copy`
  * `destinationDir` was converted from `File` to `DirectoryProperty`.

The "Gradle Next" distribution also includes features to mitigate the pain of migrating to the new APIs:

1) **bytecode upgrades** – Gradle Next can take already compiled bytecode, and replace references to old APIs with calls to new APIs transparently. 
2) **source pinning** – This feature allows a build engineer to upgrade the Gradle distribution used to build a project without having to immediately fix all the breaking changes in the build logic sources of the project.
3) **assignment source compatibility** – In Gradle Next the assignment (`=`) operator works for `Property` objects as well in both Groovy and Kotlin DSL, so the majority of existing build logic will stay source-compatible.

## Running the demo

Let's first build `test-project` with its original Gradle distribution:

```shell
$ cd test-project
$ ./gradlew assemble
>>>>>>>>>
Running with Gradle version 7.4.1
>>>>>>>>>

> Task :list:compileKotlin
Hello from Kotlin plugin, classpath length: 5

> Task :utilities:compileJava
Hello from Java plugin, classpath length: 2

> Task :app:compileGroovy
Hello from Groovy plugin, classpath length: 6

BUILD SUCCESSFUL in 667ms
32 actionable tasks: 32 executed
```

As expected, it works.

Now let's install "Gradle Next" using:

```shell
$ ./install-gradle-next.sh
```

This installs the patched distribution in the `gradle-next` directory into the `gradle-next-install` directory.

If we try to build `test-project` with Gradle Next, it will fail:

```shell
$ cd test-project
$ ../gradle-next-install/bin/gradle assemble
>>>>>>>>>
Running with Gradle version 7.6-20220603153347+0000
>>>>>>>>>

...

BUILD FAILED in 3s
4 actionable tasks: 1 executed, 3 up-to-date
```

As expected, the failures are caused by the changed APIs:

```text
> Task :build-logic:groovy-build-logic:compileGroovy FAILED
startup failed:
/Users/lptr/Workspace/gradle/provider-api-migration-testbed/test-project/build-logic/groovy-build-logic/src/main/groovy/test/project/build/TestGroovyPlugin.groovy: 15: [Static type checking] - Cannot set read-only property: destinationDir
 @ line 15, column 13.
               it.destinationDir = project.layout.buildDirectory.dir("new-resources").get().asFile
               ^

/Users/lptr/Workspace/gradle/provider-api-migration-testbed/test-project/build-logic/groovy-build-logic/src/main/groovy/test/project/build/TestGroovyPlugin.groovy: 19: [Static type checking] - Cannot find matching method org.gradle.api.provider.ListProperty#leftShift(java.lang.String). Please check if the declared type is correct and if the method exists.
 @ line 19, column 13.
               it.options.compilerArgs << "-proc:none"
               ^
```

### Bytecode upgrades

But not every API change has caused a failure.
You can see on the console a list of bytecode upgrades that happened to the Kotlin plugin:

```text
Matched call to org.gradle.api.tasks.compile.AbstractCompile.getSourceCompatibility() in org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin$createKaptKotlinTask$2$1, replacing...
Matched call to org.gradle.api.tasks.compile.CompileOptions.getCompilerArgs() in org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin$disableAnnotationProcessingInJavaTask$1, replacing...
Matched call to org.gradle.api.tasks.compile.CompileOptions.setCompilerArgs(java.util.List) in org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin$disableAnnotationProcessingInJavaTask$1, replacing...
Matched call to org.gradle.api.tasks.compile.AbstractCompile.getClasspath() in org.jetbrains.kotlin.gradle.plugin.Kotlin2JvmSourceSetProcessor$doTargetSpecificProcessing$2$1$1, replacing...
Matched call to org.gradle.api.tasks.compile.AbstractCompile.setClasspath(org.gradle.api.file.FileCollection) in org.jetbrains.kotlin.gradle.plugin.Kotlin2JvmSourceSetProcessor$doTargetSpecificProcessing$2$1$1, replacing...
Matched call to org.gradle.api.tasks.Copy.setDestinationDir(java.io.File) in org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator$Companion$createKlibArtifact$realArtifactFile$1, replacing...
Matched call to org.gradle.api.tasks.Copy.getDestinationDir() in org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator$Companion$createKlibArtifact$realArtifactFile$2, replacing...
Matched call to org.gradle.api.tasks.Copy.getDestinationDir() in org.jetbrains.kotlin.gradle.targets.js.ir.KotlinBrowserJsIr$configureBuild$2$webpackTask$1$entryFileProvider$1, replacing...
Matched call to org.gradle.api.tasks.Copy.getDestinationDir() in org.jetbrains.kotlin.gradle.targets.js.ir.KotlinBrowserJsIr$configureRun$2$runTask$1$entryFileProvider$1$1, replacing...
Matched call to org.gradle.api.tasks.Copy.getDestinationDir() in org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget$configureTestsRun$testJs$1$1, replacing...
Matched call to org.gradle.api.tasks.Copy.getDestinationDir() in org.jetbrains.kotlin.gradle.targets.js.ir.KotlinNodeJsIr$locateOrRegisterRunTask$runTaskHolder$1$1, replacing...
Matched call to org.gradle.api.tasks.compile.JavaCompile.getTargetCompatibility() in org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlinCompileConfig$2$1$1$1, replacing...
```

Here Gradle Next took the bytecode of the external plugin, and replaced references to the old APIs with calls to the new APIs to keep the plugin working.
This works with any kind of already compiled build logic code.

**Note:** the upgrades are only listed once, later executions are cached.
If you want to see them again, the easiest way is to remove `~/.gradle/caches/jar-9` before running the build again.

### Source pinning

We could now fix the problems immediately, but Gradle Next offers a way to keep the existing build logic source code working even after the upgrade. 

To make the build work with Gradle Next without changing the source code, we can pin it to the original Gradle API version.

**Note:** This requires Gradle APIs to be published to a Maven repository first.
We are planning to publish them later to Maven Central as part of the migration project.
For this demonstration we need to publish them locally:

```shell
$ cd ../gradle-repository
$ ./gradlew -PgradleApiVersion=7.4.1 publishAllPublicationsToLocalMavenRepository

...

BUILD SUCCESSFUL in 13s
15 actionable tasks: 15 executed

$ ls -l build/repo/org/gradle/gradle-api
total 40
drwxr-xr-x  12 lptr  staff  384 Jun  3 18:04 7.4.1
-rw-r--r--   1 lptr  staff  327 Jun  3 18:04 maven-metadata.xml
-rw-r--r--   1 lptr  staff   32 Jun  3 18:04 maven-metadata.xml.md5
-rw-r--r--   1 lptr  staff   40 Jun  3 18:04 maven-metadata.xml.sha1
-rw-r--r--   1 lptr  staff   64 Jun  3 18:04 maven-metadata.xml.sha256
-rw-r--r--   1 lptr  staff  128 Jun  3 18:04 maven-metadata.xml.sha512
```

Now we can invoke Gradle Next and tell it to use the old API to compile the build logic in `test-project`:

```shell
$ cd ../test-project
$ ../gradle-next-install/bin/gradle assemble \
  -Dorg.gradle.api.source-version=7.4.1 \
  -Dgradle.api.repository.url=$(pwd)/../gradle-repository/build/repo
  
>>>>>>>>>
Running with Gradle version 7.6-20220603153347+0000
>>>>>>>>>

...

> Task :list:compileKotlin
Hello from Kotlin plugin, classpath length: 5

> Task :utilities:compileJava
Hello from Java plugin, classpath length: 2

> Task :app:compileGroovy
Hello from Groovy plugin, classpath length: 6

BUILD SUCCESSFUL in 24s
32 actionable tasks: 30 executed, 2 up-to-date
```

**Note:** in this demo our goal is to demonstrate how the feature would work.
How it is going to be triggered (system property, command-line argument etc.) is not decided yet.

You can see more bytecode upgrades happening now:

```text
Matched call to org.gradle.language.jvm.tasks.ProcessResources.setDestinationDir(java.io.File) in test.project.build.TestGroovyPlugin$_apply_closure1, replacing...
Matched call to org.gradle.api.tasks.compile.CompileOptions.getCompilerArgs() in test.project.build.TestGroovyPlugin$_apply_closure2, replacing...
```

These are running against the bytcode of the build logic that wes compiled against Gradle 7.4.1, making the resulting bytecode comaptible with Gradle Next.

### Manual upgrade of sources

Let's upgrade the build logic for good.
You can follow the errors reported when trying to build without source pinning, or you can apply [this patch](test-project.patch):

```shell
$ git apply ../test-project.patch
$ git status -s
 M build-logic/groovy-build-logic/src/main/groovy/test.project.build.groovy-application-conventions.gradle
 M build-logic/groovy-build-logic/src/main/groovy/test/project/build/TestGroovyPlugin.groovy
 M build-logic/java-build-logic/src/main/java/test/project/build/TestJavaPlugin.java
 M build-logic/kotlin-build-logic/src/main/kotlin/test/project/build/TestKotlinPlugin.kt
$ ../gradle-next-install/bin/gradle assemble
>>>>>>>>>
Running with Gradle version 7.6-20220603153347+0000
>>>>>>>>>

> Task :list:compileKotlin
Hello from Kotlin plugin, classpath length: 5

> Task :utilities:compileJava
Hello from Java plugin, classpath length: 2

> Task :app:compileGroovy
Hello from Groovy plugin, classpath length: 6

BUILD SUCCESSFUL in 7s
32 actionable tasks: 19 executed, 1 from cache, 12 up-to-date
```

Everything works, we're done with the migration!

### Assignment source compatibility

Notice that we did not need to upgrade any of the assignments in Groovy or Kotlin scripts.
This is because Gradle Next adds all the syntactic sugar to make `=` work for `Property` objects.

For example, this code keeps working even when the type of `destinationDir` changes from `File` to `DirectoryProperty`:

```kotlin
project.tasks.withType<ProcessResources>().configureEach {
    destinationDir = project.layout.buildDirectory.dir("new-resources").get().asFile
}
```

Making `=` work for Kotlin DSL for `Property` types will be implemented via a new Kotlin language feature that allows the assignment operator to be overloaded.
To keep more of the existing build logic source-compatible we are planning to make `<<` and `[]` etc. work in the DSLs for multi-valued properties like `ListProperty` where they already work for their JavaBean counterparts.
