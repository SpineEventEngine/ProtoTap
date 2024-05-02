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

import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.Coroutines
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.FindBugs
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Jackson
import io.spine.internal.dependency.Kotlin
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.VersionWriter
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.github.pages.updateGitHubPages
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    id("net.ltgt.errorprone")
    id("detekt-code-analysis")
    id("pmd-settings")
    id("write-manifest")
    jacoco
    `project-report`
    idea
}
apply<IncrementGuard>()
apply<VersionWriter>()

CheckStyleConfig.applyTo(project)
JavadocConfig.applyTo(project)
LicenseReporter.generateReportIn(project)

project.run {
    addDependencies()
    forceConfigurations()

    val javaVersion = JavaLanguageVersion.of(11)
    configureJava(javaVersion)
    configureKotlin(javaVersion)

    configureTests()

    configureGitHubPages()
    configureTaskDependencies()
}

typealias Module = Project

fun Module.addDependencies() {
    dependencies {
        errorprone(ErrorProne.core)

        compileOnlyApi(FindBugs.annotations)
        compileOnlyApi(CheckerFramework.annotations)
        ErrorProne.annotations.forEach { compileOnlyApi(it) }

        testImplementation(Guava.testLib)
        JUnit.api.forEach { testImplementation(it) }
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }
}

fun Module.forceConfigurations() {
    with(configurations) {
        forceVersions()
        excludeProtobufLite()
        all {
            resolutionStrategy {
                @Suppress("DEPRECATION") // To force `Kotlin.stdLibJdk7` version.
                force(
                    Kotlin.stdLibJdk7,
                    JUnit.runner,
                    Coroutines.jdk8,
                    Coroutines.core,
                    Coroutines.bom,
                    Coroutines.coreJvm,
                    Jackson.Junior.objects,
                    Spine.reflect
                )
            }
        }
    }
}

fun Module.configureJava(javaVersion: JavaLanguageVersion) {
    java {
        toolchain.languageVersion.set(javaVersion)
    }
    tasks {
        withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
        }
    }
}

fun Module.configureKotlin(javaVersion: JavaLanguageVersion) {
    kotlin {
        applyJvmToolchain(javaVersion.asInt())
        explicitApi()
    }

    tasks {
        withType<KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = javaVersion.toString()
            setFreeCompilerArgs()
        }
    }
}

fun Module.configureTests() {
    tasks {
        registerTestTasks()
        test.configure {
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
            configureLogging()
        }
    }
}

fun Module.configureGitHubPages() {
    updateGitHubPages(Spine.ArtifactVersion.javadocTools) {
        allowInternalJavadoc.set(true)
        rootFolder.set(rootDir)
    }
}


