import java.net.URL

val kotlin_version: String by project
val ktor3_version: String by project

buildscript {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    `java-library`
    kotlin("jvm") version "2.2.20"
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    signing
    id("net.researchgate.release") version "3.0.2"
}

group = "de.sharpmind.ktor"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// For compatibility, also set Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    // make compatible with Java 17
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withJavadocJar()
    withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    // Rich version covering Ktor 3.2.x–3.6.x. Only ApplicationConfig APIs present since 3.2 are used
    // (property/propertyOrNull, config, keys, toMap, getString, getList). ApplicationConfig.getAs,
    // added in later 3.x, is intentionally not used.
    implementation("io.ktor:ktor-server-core:$ktor3_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
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
                // Gradle writes the resolved Ktor version into the POM (currently the newest 3.6.x).
                // Publish the compatibility range instead, so Maven consumers are not pinned to one patch.
                // Gradle module metadata already records `requires: [3.2.0,3.7.0)`.
                withXml {
                    val dependencies = asElement().getElementsByTagName("dependency")
                    for (i in 0 until dependencies.length) {
                        val dependency = dependencies.item(i)
                        var artifactId: String? = null
                        var versionNode: org.w3c.dom.Node? = null
                        val children = dependency.childNodes
                        for (j in 0 until children.length) {
                            val child = children.item(j)
                            if (child.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
                            when (child.nodeName) {
                                "artifactId" -> artifactId = child.textContent
                                "version" -> versionNode = child
                            }
                        }
                        if (artifactId == "ktor-server-core" || artifactId == "ktor-server-core-jvm") {
                            versionNode?.textContent = ktor3_version
                        }
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "ossrh-staging-api"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("OSSRH_USERNAME") as String? ?: System.getenv("OSSRH_USERNAME") ?: ""
                password = project.findProperty("OSSRH_PASSWORD") as String? ?: System.getenv("OSSRH_PASSWORD") ?: ""
            }
        }
    }
}


nexusPublishing {
    repositories {
        // see https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

signing {
    if (project.hasProperty("signing.keyId") &&
        project.hasProperty("signing.password") &&
        project.hasProperty("signing.secretKeyRingFile")
    ) {
        sign(publishing.publications["mavenJava"])
    }
}
