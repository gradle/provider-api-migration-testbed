plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("java-library-conventions") {
            id = "test.project.build.java-library-conventions"
            implementationClass = "test.project.build.TestJavaPlugin"
        }
    }
}