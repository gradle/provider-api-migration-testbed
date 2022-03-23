Gradle API repository
=====================

This project allows you to create a Maven file repository containing the Gradle API artifacts for a given version of Gradle.
You can specify the version of the API to add to the repository by using the Gradle property `gradleApiVersion`.

```shell
> ./gradlew -PgradleApiVersion=7.2 publishAllPublicationsToLocalMavenRepository
```

The repository is located under `build/repo`.