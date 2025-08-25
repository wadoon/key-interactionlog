/* This file is part of key-abbrevmgr.
 * key-abbrevmgr is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "2.0.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.gradleup.shadow") version "9.0.1"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "io.github.wadoon.key"
version = "1.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

repositories {
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
}

val keyVersion = System.getenv("KEY_VERSION") ?: "2.12.4-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.ajalt:clikt:2.8.0")
    implementation("org.jetbrains:annotations:26.0.2")
    implementation("com.atlassian.commonmark:commonmark:0.17.0")
    implementation("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.17.0")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.9.Final")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")

    compileOnly("org.key-project:key.core:$keyVersion")
    compileOnly("org.key-project:key.ui:$keyVersion")
    compileOnly("org.slf4j:slf4j-api:2.0.17")

    testImplementation("org.key-project:key.core:$keyVersion")
    testImplementation("org.key-project:key.ui:$keyVersion")
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
            val user: String = project.properties.getOrDefault("ossrhUsername", "").toString()
            val pwd: String = project.properties.getOrDefault("ossrhPassword", "").toString()

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
                |/* This file is part of key-abbrevmgr.
                | * key-abbrevmgr is licensed under the GNU General Public License Version 2
                | * SPDX-License-Identifier: GPL-2.0-only
                | */
            """.trimMargin(),
        ).delimiter("^(package |@file|import |plugins )")
        val editorConfigPath = File(rootDir, ".editorconfig")
        println("$editorConfigPath  ${editorConfigPath.exists()}")
        ktlint("1.7.1")
            .setEditorConfigPath(editorConfigPath)
        trimTrailingWhitespace()
        endWithNewline()
    }
}
