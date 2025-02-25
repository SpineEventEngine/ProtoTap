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

package io.spine.tools.prototap.gradle

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.tools.gradle.task.BaseTaskName.build
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.prototap.Names.GRADLE_PLUGIN_ID
import io.spine.tools.prototap.Paths.CODE_GENERATOR_REQUEST_FILE
import io.spine.tools.prototap.Paths.DESCRIPTOR_SET_FILE
import io.spine.tools.prototap.Paths.COMPILED_PROTOS_FILE
import io.spine.tools.prototap.Paths.outputRoot
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Gradle plugin should")
internal class PluginSpec {

    private lateinit var projectDir: File
    private lateinit var project: GradleProject
    private lateinit var resultDir: Path

    @BeforeEach
    fun prepareDir(@TempDir tempDir: File) {
        projectDir = tempDir
        val buildDir = "$projectDir/build"
        resultDir = outputRoot(buildDir)
    }

    @Test
    fun `run with default values`() {
        createProject("default-values")
        runBuild()
        assertJavaCodeGenerated()
        assertRequestFileExits()
        assertCompiledProtosFileExists()
    }

    @Test
    fun `run with proto files under 'tests'`() {
        createProject("proto-in-test")
        runBuild()
        assertJavaCodeGenerated()
        assertRequestFileExits()
        assertCompiledProtosFileExists()
    }

    @Test
    fun `run with 'prototap' settings`() {
        createProject("with-settings")
        runBuild()
        assertJavaCodeGenerated()
        assertRequestFileExits()
        assertCompiledProtosFileExists()
        assertDescriptorSetFileExits()
    }

    @Test
    fun `work as a 'classpath' dependency of 'buildscript'`() {
        createProject("via-classpath")
        runBuild()
        assertJavaCodeGenerated()
        assertRequestFileExits()
        assertCompiledProtosFileExists()
    }

    @Test
    fun `run when plugin is added before 'java-test-fixtures'`() {
        createProject("before-test-fixtures")
        runBuild()
        assertJavaCodeGenerated()
        assertRequestFileExits()
        assertCompiledProtosFileExists()
    }

    private fun assertJavaCodeGenerated() {
        val javaDir = resultDir.resolve("java")
        javaDir.countFiles() shouldNotBe 0
        val packageDir = javaDir.resolve("io/spine/given/domain/gas/")
        packageDir.run {
            exists() shouldBe true
            // Check some Java files too.
            resolve("Pipe.java").exists() shouldBe true
            resolve("CompressorStation.java").exists() shouldBe true
            resolve("LngStorage.java").exists() shouldBe true
        }
    }

    private fun assertRequestFileExits() {
        resultDir.resolve(CODE_GENERATOR_REQUEST_FILE).exists() shouldBe true
    }

    private fun assertDescriptorSetFileExits() {
        resultDir.resolve(DESCRIPTOR_SET_FILE).exists() shouldBe true
    }

    private fun assertCompiledProtosFileExists() {
        resultDir.resolve(COMPILED_PROTOS_FILE).exists() shouldBe true
    }

    private fun createProject(resourceDir: String) {
        val version = Plugin.readVersion()
        val builder = GradleProject.setupAt(projectDir)
            .fromResources(resourceDir)
            .withSharedTestKitDirectory()
            .replace("@PROTOTAP_PLUGIN_ID@", GRADLE_PLUGIN_ID)
            .replace("@PROTOTAP_VERSION@", version)
            /* Uncomment the following if you need to debug the build process.
               Please note that:
                 1) Test will run much slower.
                 2) Under Windows it may cause this issue to occur:
                    https://github.com/gradle/native-platform/issues/274
               After finishing the debug, please comment out this call again. */
            //.enableRunnerDebug()
            //.withLoggingLevel(LogLevel.INFO)
            .copyBuildSrc()
        project = builder.create()
        (project.runner as DefaultGradleRunner).withJvmArguments(
            "-Xmx8g",
            "-XX:MaxMetaspaceSize=1512m",
            "-XX:+UseParallelGC",
            "-XX:+HeapDumpOnOutOfMemoryError"
        )
    }

    private fun runBuild() {
        project.executeTask(build)
    }
}

private fun Path.countFiles(): Int =
    toFile().walk().count { it.isFile }
