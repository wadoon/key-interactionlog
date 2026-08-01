import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "2.2.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.gradleup.shadow") version "9.6.1"
    id("io.github.ben-manes.versions") version "0.58.0"
}

group = "io.github.wadoon.key"
version = "0.9-SNAPSHOT"

repositories {
    mavenCentral()
}

repositories {
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
}

val keyVersion = System.getenv("KEY_VERSION") ?: "3.0.0"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
    implementation("com.github.ajalt:clikt:2.8.0")
    implementation("org.jetbrains:annotations:26.1.0")
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
                    connection.set("scm:git:git://github.com/wadoon/key-interactionlog.git")
                    developerConnection.set("scm:git:git://github.com/wadoon/key-interactionlog.git")
                    url.set("https://github.com/wadoon/key-interactionlog")
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

    tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
        fun String.isNonStable(): Boolean {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isStable = stableKeyword || regex.matches(this)
            return isStable.not()
        }

        rejectVersionIf {
            candidate.version.isNonStable()
        }

        revision = "release"
    }