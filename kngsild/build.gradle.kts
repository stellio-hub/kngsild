import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"

    kotlin("kapt") version "1.9.22"
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

val arrowVersion = "1.2.1"
val jacksonVersion = "2.16.1"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrowVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

version = "0.4.0-dev"
group = "io.egm"

publishing {
    publications {
        create<MavenPublication>("kngsild") {
            from(components["java"])
        }
    }
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

tasks.withType<Detekt>().configureEach {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    baseline.set(file("$projectDir/config/detekt/baseline.xml"))

    reports {
        xml.required.set(true)
        txt.required.set(false)
        html.required.set(true)
    }
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig.set(true)
    baseline.set(file("$projectDir/config/detekt/baseline.xml"))
}
