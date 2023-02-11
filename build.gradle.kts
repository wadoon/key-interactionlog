import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"

    id("org.jetbrains.dokka") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"

    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"

    `java-library`
    `maven-publish`
    `signing`
}

group = "io.github.wadoon"
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
    maven("https://git.key-project.org/api/v4/projects/35/packages/maven")
}

dependencies {
    val implementation by configurations


    plugin(platform("org.jetbrains.kotlin:kotlin-bom"))
    plugin("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    plugin("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    plugin("com.github.ajalt:clikt:2.8.0")
    plugin("org.jetbrains:annotations:23.0.0")
    plugin("com.atlassian.commonmark:commonmark:0.17.0")
    plugin("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.17.0")
    plugin("org.ocpsoft.prettytime:prettytime:5.0.2.Final")
    plugin("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    //    implementation("org.key_project:key.core")

    val testImplementation by configurations

    implementation("org.key_project:key.core:2.11.0-SNAPSHOT")
    implementation("org.key_project:key.ui:2.11.0-SNAPSHOT")
    implementation("org.key_project:key.util:2.11.0-SNAPSHOT")

    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("org.slf4j:slf4j-simple:1.7.33")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")


}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<JavaCompile> {
    options.release.set(11)
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

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}


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

