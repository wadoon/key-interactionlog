
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"

    id("org.jetbrains.dokka") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"

    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"

    `java-library`
    `maven-publish`
    `signing`
}

group = "io.github.wadoon.key"
version = "0.9-SNAPSHOT"

repositories {
    mavenCentral()
}

val plugin by configurations.creating

configurations {
    implementation.get().extendsFrom(plugin)
}

tasks.getByName<ShadowJar>("shadowJar") {
    configurations = listOf(plugin)
}

repositories {
    mavenCentral()
    //maven("https://git.key-project.org/api/v4/projects/35/packages/maven")
}

dependencies {
    val implementation by configurations


    plugin(platform("org.jetbrains.kotlin:kotlin-bom"))
    plugin("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    plugin("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    plugin("com.github.ajalt:clikt:2.8.0")
    plugin("org.jetbrains:annotations:26.0.2")
    plugin("com.atlassian.commonmark:commonmark:0.17.0")
    plugin("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.17.0")
    plugin("org.ocpsoft.prettytime:prettytime:5.0.9.Final")
    plugin("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")

    //    implementation("org.key_project:key.core")

    implementation("org.key-project:key.core:2.12.3")
    implementation("org.key-project:key.ui:2.12.3")
    implementation("org.key-project:key.util:2.12.3")

    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
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

/*
nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
*/

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                description.set("Interaction Logging plugin for the KeY Theorem Prover")
                url.set("https://github.com/wadoon/key-interactionlog")
                licenses {
                    license {
                        name.set("GNU Public License Version 2")
                        url.set("https://www.gnu.org/licenses/old-licenses/gpl-2.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("wadoon")
                        name.set("Alexander Weigl")
                        email.set("weigl@kit.edu")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/wadoon/key-interactionlog.git")
                    developerConnection.set("scm:git:git://github.com/wadoon/key-interactionlog.git")
                    url.set("https://github.com/wadoon/key-interactionlog")
                }
            }
        }
    }
}

