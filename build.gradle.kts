import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlin_version: String by project
val ktor2_version: String by project

buildscript {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    `java-library`
    kotlin("jvm") version "1.8.0"

    `maven-publish`
    signing
    id("net.researchgate.release") version "3.0.2"
}

group = "de.sharpmind.ktor"

repositories {
    mavenCentral()
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-core:$ktor2_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Ktor Environment Config")
                description.set("Environment aware configuration for Ktor")
                url.set("https://github.com/sharpmind-de/ktor-env-config")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("stolzem")
                        name.set("Marco Stolze")
                        email.set("marco@sharpmind.de")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/sharpmind-de/ktor-env-config.git")
                    developerConnection.set("scm:git:https://github.com/sharpmind-de/ktor-env-config.git")
                    url.set("https://github.com/sharpmind-de/ktor-env-config")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("OSSRH_USERNAME") as String? ?: System.getenv("OSSRH_USERNAME") ?: ""
                password = project.findProperty("OSSRH_PASSWORD") as String? ?: System.getenv("OSSRH_PASSWORD") ?: ""
            }
        }
    }
}

signing {
    if (project.hasProperty("signing.keyId") &&
            project.hasProperty("signing.password") &&
            project.hasProperty("signing.secretKeyRingFile")) {
        sign(publishing.publications["mavenJava"])
    }
}
