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

@file:Suppress(
    "UnstableApiUsage" /* `ProcessResources` task we use is marked `@Incubating`. */,
    "TooManyFunctions" /* Smaller functions are used for better readability. */
)

package io.spine.tools.prototap.gradle

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.id
import io.spine.tools.code.manifest.Version
import io.spine.tools.gradle.Artifact
import io.spine.tools.gradle.artifact
import io.spine.tools.gradle.protobuf.ProtobufDependencies
import io.spine.tools.gradle.protobuf.protobufExtension
import io.spine.tools.prototap.Names.GRADLE_EXTENSION_NAME
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_CLASSIFIER
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import io.spine.tools.prototap.Paths.CODE_GENERATOR_REQUEST_FILE
import io.spine.tools.prototap.Paths.DESCRIPTOR_SET_FILE
import io.spine.tools.prototap.Paths.TARGET_DIR
import io.spine.tools.prototap.Paths.interimDir
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.pathString
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * A Gradle plugin which adds ProtoTap plugin to `protoc` and configures the project
 * tasks for copying generated source code and other related files into test resources.
 */
public class Plugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        createExtension()
        pluginManager.withPlugin(ProtobufDependencies.gradlePlugin.id) {
            tapProtobuf()
        }
    }

    public companion object {

        /**
         * Reads the version of the plugin from the resources.
         */
        @JvmStatic
        @VisibleForTesting
        public fun readVersion(): String =
            Version.fromManifestOf(
                // Use the fully qualified name of our `Plugin` class so that we load
                // the version for the manifest of our JAR.
                // Short name would refer to the imported `Plugin` class from Gradle, and
                // we'd get its version.
                io.spine.tools.prototap.gradle.Plugin::class.java
            ).value
    }
}

private fun Project.createExtension(): Extension {
    val extension = Extension(this)
    extensions.add(Extension::class.java, GRADLE_EXTENSION_NAME, extension)
    return extension
}

/**
 * Tunes this Gradle project so that we can collect data from `protoc` and
 * Protobuf Gradle Plugin.
 */
private fun Project.tapProtobuf() {
    createProtocPlugin()
    tuneProtoTasks()
}

/**
 * Obtains the extension our plugin added to this Gradle project.
 */
private val Project.extension: Extension
    get() = extensions.getByType(Extension::class.java)

private fun Project.createProtocPlugin() = protobufExtension?.run {
    plugins {
        it.create(PROTOC_PLUGIN_NAME) { locator ->
            locator.artifact = protocPlugin.notation()
        }
    }
}

/**
 * The Maven artifact of the ProtoTap plugin for `protoc`.
 */
private val protocPlugin: Artifact by lazy {
    artifact {
        useSpineToolsGroup()
        name = "prototap-protoc-plugin"
        version = io.spine.tools.prototap.gradle.Plugin.readVersion()
        classifier = PROTOC_PLUGIN_CLASSIFIER
        extension = "jar"
    }
}

private fun Project.tuneProtoTasks() {
    val sourceSetName = extension.sourceSet.get().name

    /* The below block adds a configuration action for the `GenerateProtoTaskCollection`.
       We cannot do it like `generateProtoTasks.all().forEach { ... }` because it
       breaks the configuration order of the `GenerateProtoTaskCollection`.
       This, in turn, leads to missing generated sources in the `compileJava` task. */
    protobufExtension?.generateProtoTasks {
        it.ofSourceSet(sourceSetName).configureEach { task ->
            tasks.processTestResources.run {
                copySourcesFrom(task.outputBaseDir)
                copyProtocPluginOutput()
            }
            task.apply {
                addProtocPlugin()
                grabDescriptorSetFile()
            }
        }
    }
}

private val Project.interimDir: Path
    get() = interimDir(buildDir.path)

private fun Project.interimFile(name: String): String =
    interimDir.resolve(name).pathString

private val Project.codeGeneratorRequestFile: String
    get() = interimFile(CODE_GENERATOR_REQUEST_FILE)

private val Project.descriptorSetFile: String
    get() = interimFile(DESCRIPTOR_SET_FILE)

private fun GenerateProtoTask.addProtocPlugin() {
    plugins.apply {
        id(PROTOC_PLUGIN_NAME) {
            val path = project.codeGeneratorRequestFile
            val encoded = path.base64Encoded()
            option(encoded)
        }
    }
}

private fun ProcessResources.copySourcesFrom(directory: String) {
    from(directory) { spec ->
        // Exclude the empty directory automatically created by
        // Protobuf Gradle Plugin for our `protoc` plugin.
        spec.exclude(PROTOC_PLUGIN_NAME)
        spec.into(TARGET_DIR)
    }
}

private fun ProcessResources.copyProtocPluginOutput() {
    from(project.interimDir) { spec ->
        spec.into(TARGET_DIR)
    }
}

private fun GenerateProtoTask.grabDescriptorSetFile() {
    if (project.extension.generateDescriptorSet.get()) {
        generateDescriptorSet = true
        descriptorSetOptions.apply {
            path = project.descriptorSetFile
            includeSourceInfo = true
            includeImports = true
        }
    }
}

private val TaskContainer.processTestResources: ProcessResources
    get() = named("processTestResources", ProcessResources::class.java).get()

private fun String.base64Encoded(): String {
    val bytes = encodeToByteArray()
    return Base64.getEncoder().encodeToString(bytes)
}
