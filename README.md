# Introduction

kngsild is a Kotlin library exposing NGSI-LD API endpoints usable from any JVM compatible code.

It is still in early development, so a lot of endpoints are not yet implemented. But new ones are added regularly,
and you are of course welcome to raise a PR if you implement a new one. 

# Building and publishing

The library is published with Jitpack and available in https://jitpack.io/#stellio-hub/kngsild. 

When working locally, you can publish it in your local Maven repository by running the following command:

```shell
./gradlew publishToMavenLocal
```

It will then be stored in `~/.m2/repository/io/egm/kngsild/<version>/kngsild-<version>.jar` and referencable in a
Gradle build file with the following dependency statement:

```
implementation 'io.egm:kngsild:<version>'
```
