plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.16.0"

    kotlin("kapt") version "1.4.31"
    `java-library`
}

repositories {
    // Use JCenter for resolving dependencies.
    jcenter()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/arrow-kt/arrow-kt/") }
}

val arrowVersion = "0.11.0"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
    "kapt"("io.arrow-kt:arrow-meta:$arrowVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
    implementation("org.slf4j:slf4j-log4j12:1.7.30")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
    testImplementation("org.mockito:mockito-inline:3.8.0")
}

version = "0.1.6"
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
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
