plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.4.20"

    `java-library`
    `maven-publish`
}

repositories {
    // Use JCenter for resolving dependencies.
    jcenter()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("com.google.guava:guava:29.0-jre")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.apache.commons:commons-math3:3.6.1")
}

version = "0.1.0"
group = "io.egm.kngsild"

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Title" to "kngsild",
            "Implementation-Version" to project.version))
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("kngsild") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            credentials {
                username = "username"
                password = "password"
            }

            url = uri("https://maven.pkg.jetbrains.space/mycompany/p/projectkey/my-maven-repo")
        }
    }
}
