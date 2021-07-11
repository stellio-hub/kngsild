# Introduction

kngsild is a Kotlin library exposing NGSI-LD API endpoints usable from any JVM compatible code.

It is still in early development, so a lot of endpoints are not yet implemented. But new ones are added regularly,
and you are of course welcome to raise a PR if you implement a new one. 

# Building and publishing

The library is published with Jitpack and available in https://jitpack.io/#io.egm/kngsild. 

When working locally, you can publish it in your local Maven repository by running the following command:

```shell
./gradlew publishToMavenLocal
```

It is then stored in `~/.m2/repository/io/egm/kngsild/<version>/kngsild-<version>.jar` and can be used in your project
in the same way as when it is published on Jitpack.
