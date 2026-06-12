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

@file:Suppress("AvoidApplyPluginMethod") // We apply some plugins via classes, not by id.

import com.google.common.io.Files
import io.spine.dependency.boms.BomsPlugin
import io.spine.dependency.build.CheckerFramework
import io.spine.dependency.build.ErrorProne
import io.spine.dependency.build.FindBugs
import io.spine.dependency.build.Ksp
import io.spine.dependency.lib.Caffeine
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Guava
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.lib.Roaster
import io.spine.dependency.local.Base
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.test.JUnit
import io.spine.gradle.checkstyle.CheckStyleConfig
import io.spine.gradle.github.pages.updateGitHubPages
import io.spine.gradle.javac.configureErrorProne
import io.spine.gradle.javac.configureJavac
import io.spine.gradle.javadoc.JavadocConfig
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.publish.IncrementGuard
import io.spine.gradle.report.license.LicenseReporter
import java.util.*

plugins {
    java
    `java-library`
    `java-test-fixtures`
    kotlin("jvm")
    id("write-manifest")
    id("com.google.protobuf")
    id("module-testing")
    id("dokka-setup")
    id("net.ltgt.errorprone")
    id("pmd-settings")
    `maven-publish`
    id("detekt-code-analysis")
    id("org.jetbrains.kotlinx.kover")
    id("project-report")
    idea
}
apply<BomsPlugin>()
apply<IncrementGuard>()

LicenseReporter.generateReportIn(project)
JavadocConfig.applyTo(project)
CheckStyleConfig.applyTo(project)

project.run {
    addDependencies()
    forceConfigurations()

    val javaVersion = BuildSettings.javaVersion
    configureJava(javaVersion)
    configureKotlin()
    setupTests()

    val generatedDir = "$projectDir/generated"
    val generatedResources = "$generatedDir/main/resources"
    prepareProtocConfigVersionsTask(generatedResources)
    setupSourceSets(generatedResources)

    configureTaskDependencies()
    setupDocPublishing()
}

typealias Module = Project

fun Module.addDependencies() {
    dependencies {
        errorprone(ErrorProne.core)

        compileOnlyApi(FindBugs.annotations)
        compileOnlyApi(CheckerFramework.annotations)
        ErrorProne.annotations.forEach { compileOnlyApi(it) }

        implementation(Guava.lib)
        implementation(Logging.lib)
    }
}

fun Module.forceConfigurations() {
    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            // Exclude outdated module.
            exclude(group = "io.spine", module = "spine-logging-backend")

            // Exclude in favor of `spine-validation-java-runtime`.
            exclude("io.spine", "spine-validate")
            resolutionStrategy {
                dependencySubstitution {
                    // Substitute the legacy artifact coordinates with the new `ToolBase.lib` alias.
                    substitute(module("io.spine.tools:spine-tool-base"))
                        .using(module(ToolBase.lib))
                    substitute(module("io.spine.tools:spine-plugin-base"))
                        .using(module(ToolBase.pluginBase))
                }

                val rs = this@resolutionStrategy
                val cfg = this@all
                Grpc.forceArtifacts(project, cfg, rs)
                Ksp.forceArtifacts(project, cfg, rs)
                Jackson.forceArtifacts(project, cfg, rs)
                Jackson.DataFormat.forceArtifacts(project, cfg, rs)
                Jackson.DataType.forceArtifacts(project, cfg, rs)
                force(
                    Grpc.bom,
                    Jackson.bom,
                    Jackson.annotations,
                    JUnit.bom,
                    Kotlin.bom,
                    Kotlin.Compiler.embeddable,
                    Kotlin.scriptRuntime,
                    Kotlin.GradlePlugin.api,
                    KotlinPoet.ksp,
                    KotlinPoet.lib,
                    Protobuf.compiler,
                    Caffeine.lib,
                    Reflect.lib,
                    Roaster.api,
                    Roaster.jdt,
                    Base.annotations,
                    Base.lib,
                    Base.environment,
                    Base.format,
                    Time.lib,
                    Time.javaExtensions,
                    Compiler.api,
                    Compiler.backend,
                    Compiler.params,
                    Compiler.gradleApi,
                    Compiler.pluginLib,
                    CoreJvm.core,
                    CoreJvm.client,
                    CoreJvm.server,
                    TestLib.lib,
                    ToolBase.lib,
                    ToolBase.classicCodegen,
                    ToolBase.pluginBase,
                    ToolBase.jvmTools,
                    ToolBase.gradlePluginApi,
                    ToolBase.intellijPlatform,
                    ToolBase.intellijPlatformJava,
                    ToolBase.protobufSetupPlugins,
                    ToolBase.psiJava,
                    Logging.lib,
                    Logging.libJvm,
                    Logging.testLib,
                    Logging.grpcContext,
                    Compiler.api,
                    Compiler.gradleApi,
                    Compiler.jvm,
                    Validation.javaBundle,
                    Validation.runtime,
                )
            }
        }
    }
}

fun Module.configureJava(javaVersion: JavaLanguageVersion) {
    tasks.withType<JavaCompile>().configureEach {
        val javaVer = javaVersion.toString()
        sourceCompatibility = javaVer
        targetCompatibility = javaVer
        configureJavac()
        configureErrorProne()
    }
}

fun Module.configureKotlin() {
    kotlin {
        explicitApi()
        compilerOptions {
            jvmTarget.set(BuildSettings.jvmTarget)
            setFreeCompilerArgs()
        }
    }
}

fun Module.setupTests() {
    tasks {
        withType<Test> {
            // See https://github.com/gradle/gradle/issues/18647.
            jvmArgs(
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/java.util=ALL-UNNAMED",
                // Entries required for ErrorProne.
                "--add-opens", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED"
            )
        }
    }
}

fun Module.prepareProtocConfigVersionsTask(generatedResources: String) {
    val prepareProtocConfigVersions by tasks.registering {
        description = "Prepares the versions.properties file."

        val propertiesFile = file("$generatedResources/versions.properties")
        outputs.file(propertiesFile)

        val versions = Properties().apply {
            setProperty("baseVersion", Base.version)
            setProperty("protobufVersion", Protobuf.version)
            setProperty("gRPCVersion", Grpc.version)
        }

        @Suppress("UNCHECKED_CAST")
        inputs.properties(HashMap(versions) as MutableMap<String, *>)

        doLast {
            Files.createParentDirs(propertiesFile)
            propertiesFile.createNewFile()
            propertiesFile.outputStream().use {
                versions.store(it,
                    "Versions of dependencies of the Spine Model Compiler for Java plugin and" +
                            " the Spine Protoc plugin.")
            }
        }
    }

    tasks.processResources {
        dependsOn(prepareProtocConfigVersions)
    }
}

fun Module.setupSourceSets(generatedResources: String) {
    sourceSets.main {
        resources.srcDir(generatedResources)
    }
}

/**
 * Configures documentation publishing for this subproject.
 */
fun Module.setupDocPublishing() {
    updateGitHubPages {
        rootFolder.set(rootDir)
    }

    tasks.named("publish") {
        dependsOn("${project.path}:updateGitHubPages")
    }
}
