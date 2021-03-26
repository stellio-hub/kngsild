plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.16.0"

    `java-library`
    `maven-publish`
}

repositories {
    // Use JCenter for resolving dependencies.
    jcenter()
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

version = "0.1.0"
group = "io.egm.kngsild"

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "kngsild",
                "Implementation-Version" to project.version
            )
        )
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
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/stellio-hub/kngsild")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

detekt {
    toolVersion = "1.16.0"
    input = files("src/main/kotlin", "src/test/kotlin")
    config = files("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    baseline = file("$projectDir/config/detekt/baseline.xml")

    reports {
        xml.enabled = true
        txt.enabled = false
        html.enabled = true
    }
}
