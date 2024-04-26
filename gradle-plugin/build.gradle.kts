/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Kotlin
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.isSnapshot
import io.spine.internal.gradle.publish.SpinePublishing

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
//                implementation(Spine.pluginTestlib)
            }
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(JUnit.version)
            dependencies {
                implementation(Kotlin.gradlePluginLib)
                implementation(Kotlin.testJUnit5)
//                implementation(Spine.testlib)
//                implementation(Spine.pluginTestlib)
                implementation(project(":gradle-plugin"))
            }
        }
    }
}

/**
 * Make functional tests depend on publishing all the submodules to Maven Local so that
 * the Gradle plugin can get all the dependencies when it's applied to the test projects.
 */
val functionalTest: Task by tasks.getting {
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
}

gradlePlugin {
    website.set("https://spine.io")
    vcsUrl.set("https://github.com/SpineEventEngine/ProtoTap")
    plugins {
        create("prototapPlugin") {
            id = "io.spine.prototap"
            implementationClass = "io.spine.tools.prototap.gradle.Plugin"
            displayName = "ProtoTap Gradle Plugin"
            description = "Obtains generated code and related data from Protobuf compiler"
            tags.set(listOf("protobuf", "protoc", "codegen", "gradle", "plugin"))
        }
    }
    val functionalTest by sourceSets.getting
    testSourceSets(
        functionalTest
    )
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
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }

    ideaModule {
        notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/13480")
    }

    publishPlugins {
        notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/21283")
    }
}
