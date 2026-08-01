/* This file is part of key-interactionlog.
 * key-interactionlog is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
import java.net.URI

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "2.2.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.gradleup.shadow") version "9.6.1"
    id("com.diffplug.spotless") version "8.9.0"
}

group = "io.github.wadoon.key"
version = "1.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
}

val keyVersion = System.getenv("KEY_VERSION") ?: "3.0.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
    // implementation("com.github.ajalt:clikt:2.8.0")
    // implementation("org.jetbrains:annotations:26.1.0")
    implementation("com.atlassian.commonmark:commonmark:0.17.0")
    implementation("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.17.0")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.9.Final")
    // implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")

    compileOnly("org.key-project:key.core:$keyVersion")
    compileOnly("org.key-project:key.ui:$keyVersion")
    compileOnly("org.slf4j:slf4j-api:2.0.18")

    testImplementation("org.key-project:key.core:$keyVersion")
    testImplementation("org.key-project:key.ui:$keyVersion")
    testImplementation("com.google.truth:truth:1.4.5")
    testImplementation("org.slf4j:slf4j-simple:2.0.18")

    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.register<JavaExec>("run") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "io.github.wadoon.key.interactionlog.ManualTest"
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports.html.required.set(false)
    reports.junitXml.required.set(true)
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Javadoc> {
    isFailOnError = false
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = project.name
                description = "Interaction Logging plugin for the KeY Theorem Prover"
                url = "https://github.com/wadoon/key-interactionlog"
                licenses {
                    license {
                        name = "GNU Public License Version 2"
                        url = "https://www.gnu.org/licenses/old-licenses/gpl-2.0.html"
                    }
                }
                developers {
                    developer {
                        id = "wadoon"
                        name = "Alexander Weigl"
                        email = "weigl@kit.edu"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/wadoon/key-interactionlog.git"
                    developerConnection = "scm:git:git://github.com/wadoon/key-interactionlog.git"
                    url = "https://github.com/wadoon/key-interactionlog"
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        create("central") {
            nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
            snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")

            stagingProfileId.set("io.github.wadoon")
            val user: String = project.findProperty("ossrhUsername")?.toString() ?: ""
            val pwd: String = project.findProperty("ossrhPassword")?.toString() ?: ""

            username.set(user)
            password.set(pwd)
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

// version and style are optional
spotless {
    kotlin {
        target("**/*.kt", "**/*.kts")
        licenseHeader(
            """
                |/* This file is part of key-interactionlog.
                | * key-interactionlog is licensed under the GNU General Public License Version 2
                | * SPDX-License-Identifier: GPL-2.0-only
                | */
            """.trimMargin(),
        ).delimiter("^(package |@file|import |plugins )")
        val editorConfigPath = File(rootDir, ".editorconfig")
        ktlint("1.7.1")
            .setEditorConfigPath(editorConfigPath)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

fun makeDependencyUrl(
    group: String,
    name: String,
    version: String,
): URI {
    val url =
        project.repositories
            .mavenCentral()
            .url
            .toString()
            .trimEnd('/')
    val g = group.replace('.', '/')
    return uri("$url/$g/$name/$version/$name-$version.jar")
}

fun makeDependencyUrl(it: Dependency): URI = makeDependencyUrl(it.group ?: "", it.name, it.version ?: "")

fun dependenciesURLs(): Sequence<URI> {
    val dependencies =
        project.configurations
            .getByName("implementation")
            .dependencies
            .asSequence()
    return dependencies.map(::makeDependencyUrl) +
        sequenceOf(
            makeDependencyUrl(
                project.group.toString(),
                project.name,
                project.version.toString(),
            ),
        )
}

tasks.register("makeDownloadScript") {
    outputs.file("download.sh")

    doLast {
        val dependenciesURLs = dependenciesURLs()
        dependenciesURLs.forEach {
            println(it)
        }

        file("download.sh").bufferedWriter().use { out ->
            fun URI.filename(): String = this.path.takeLastWhile { it != '/' }
            val downloadedJars =
                dependenciesURLs.map {
                    $$"$TARGET/" + it.filename()
                }

            val downloads =
                dependenciesURLs
                    .zip(downloadedJars)
                    .joinToString("") { (u, f) ->
                        "|download '$u'\\\n" +
                            "|         \"$f\"\n"
                    }

            out.write(
                $$"""
                |#!/bin/sh

                |TARGET=$(readlink -f "$${project.name}-$${project.version}")

                |mkdir -p $TARGET
                |function download() {
                |    # Use curl or wget
                |    if command -v wget >/dev/null 2>&1; then
                |        wget -timestamping -O    "$2" "$1"
                |    elif command -v curl >/dev/null 2>&1; then
                |        curl -L -o "$2" "$1"
                |    else
                |        echo "Error: Neither curl nor wget is installed."
                |        exit 1
                |    fi
                |}

                $$downloads

                |echo "Extend your classpath by following Jars, either using the whole folder, or by single Jars."
                |echo "java -cp key-3.0.0-dev.jar:$TARGET/*"
                |echo "or: java -cp key-3.0.0.jar:$${downloadedJars.joinToString(":")}"
                """.trimMargin(),
            )
        }
    }
}
