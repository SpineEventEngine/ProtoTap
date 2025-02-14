/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.spine.dependency.test.JUnit
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Spine
import io.spine.gradle.isSnapshot
import io.spine.gradle.publish.SpinePublishing

plugins {
    `java-gradle-plugin`
    `maven-publish`
    `version-to-resources`
    `write-manifest`
    id("com.gradle.plugin-publish") version "1.2.1"
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly(Protobuf.GradlePlugin.lib)

    api(project(":api"))
    implementation(Spine.pluginBase)
    implementation(Kotlin.gradlePluginApi)
}

@Suppress(
    "UnstableApiUsage" /* testing suites feature */
)
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(JUnit.version)
            dependencies {
                implementation(Kotlin.gradlePluginLib)
                implementation(gradleKotlinDsl())
                implementation(Protobuf.GradlePlugin.lib)
                implementation(Spine.pluginTestlib)
            }
        }
    }
}

/**
 * Make functional tests depend on publishing all the submodules to Maven Local so that
 * the Gradle plugin can get all the dependencies when it's applied to the test projects.
 */
val test: Task by tasks.getting {
    val task = this
    rootProject.subprojects.forEach { subproject ->
        task.dependsOn(":${subproject.name}:publishToMavenLocal")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

val shadowJar by tasks.getting(ShadowJar::class) {
    archiveClassifier.set("")
    exclude(
        "org/checkerframework/**",
        "org/jboss/**",

        // Exclude license files that cause or may cause issues with LicenseReport.
        // We analyze these files when building artifacts we depend on.
        "about_files/**",

        "ant_tasks/**", // `resource-ant.jar` is of no use here.
        
        // Protobuf files.
        "google/**",
        "spine/**",
        "src/**",

        // Java source code files of the package `org.osgi`.
        "OSGI-OPT/**",

        // Unnecessary settings from the Eclipse Platform.
        "OSGI-INF/**",

        "META-INF/com.android.tools/**",
        "META-INF/maven/**",
        "META-INF/native-image/**",
        "META-INF/proguard/**",
        "META-INF/services/org.jboss.forge.roaster.*.*",
        "META-INF/eclipse.inf",

        // Checker Framework license.
        "META-INF/LICENSE.txt",

        // OSGi notices
        "META-INF/NOTICE",

        // Unnecessary stuff from the Eclipse Platform and other dependencies.
        ".api_description",
        ".options",
        "_base_main_unspecified.desc",
        "_base_main_unspecified.desc",
        "about.html",
        "profile.plist",
        "*.profile",
        "jdtCompilerAdapter.jar",
        "plugin.properties",
        "plugin.xml",
        "systembundle.properties",
    )
}

@Suppress("UnstableApiUsage") // `@Incubating` properties of `gradlePlugin`.
gradlePlugin {
    website.set("https://spine.io")
    vcsUrl.set("https://github.com/SpineEventEngine/ProtoTap")
    plugins {
        create("prototapPlugin") {
            id = "io.spine.prototap"
            implementationClass = "io.spine.tools.prototap.gradle.Plugin"
            displayName = "ProtoTap Gradle Plugin"
            description = "Obtains generated code and related data from Protobuf compiler."
            tags.set(listOf("protobuf", "protoc", "prototap", "codegen"))
        }
    }
}

// Add the common prefix to the `pluginMaven` publication.
//
// The publication is automatically created in `project.afterEvaluate` by Plugin Publishing plugin.
// See https://docs.gradle.org/current/userguide/java_gradle_plugin.html#maven_publish_plugin
//
// We add the prefix also in `afterEvaluate` assuming we're later in the sequence of
// the actions and the publications have been already created.
//
project.afterEvaluate {
    publishing {
        // Get the prefix used for all the modules of this project.
        val prefix = project.rootProject.the<SpinePublishing>().artifactPrefix

        // Add the prefix to the `pluginMaven` publication only.
        publications.named<MavenPublication>("pluginMaven") {
            if (!artifactId.startsWith(prefix)) {
                artifactId = prefix + artifactId
            }
        }

        // Do not add the prefix for the publication which produces
        // the `io.spine.prototap.gradle.plugin` marker.

        // Add the prefix to the `artifactId` of the plugin dependency used in the marker.
        publications.withType<MavenPublication>().configureEach {
            if (name.endsWith("PluginMarkerMaven")) {
                pom.withXml {
                    asNode().apply {
                        val root = asElement()
                        // Get the `dependencies/dependency/artifactId` node.
                        // It's the second node with such a name from the top.
                        // The first one is the `artifactId` of the marker itself.
                        val artifactIdNode = root.getElementsByTagName("artifactId").item(1)
                        artifactIdNode.textContent = "$prefix${project.name}"
                    }
                }
            }
        }
    }
}

// The version declared in `version.gradle.kts`.
val versionToPublish: String by extra

// Do not attempt to publish snapshot versions to comply with publishing rules.
// See: https://plugins.gradle.org/docs/publish-plugin#approval
val publishPlugins: Task by tasks.getting {
    enabled = !versionToPublish.isSnapshot()
}

val publish: Task by tasks.getting {
    dependsOn(publishPlugins)
}

tasks {
    ideaModule {
        notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/13480")
    }

    publishPlugins {
        notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/21283")
    }
}

configureTaskDependencies()
