
abstract class MyTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract var input: File

    @TaskAction
    fun printStuff() {
        println("Input is ${input.absolutePath}")
    }
}

tasks.register<MyTask>("myTask") {
    input = file("some-input")
}