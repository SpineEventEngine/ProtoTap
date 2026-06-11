/*
 * Copyright 2026, TeamDev. All rights reserved.
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

@file:Suppress(
    "UnstableApiUsage" /* `ProcessResources` task we use is marked `@Incubating`. */,
    "TooManyFunctions" /* Smaller functions are used for better readability. */
)

package io.spine.tools.prototap.gradle

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.id
import io.spine.tools.meta.MavenArtifact
import io.spine.tools.prototap.Names.GRADLE_EXTENSION_NAME
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_CLASSIFIER
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import io.spine.tools.prototap.Paths.CODE_GENERATOR_REQUEST_FILE
import io.spine.tools.prototap.Paths.CODE_GENERATOR_REQUEST_JSON_FILE
import io.spine.tools.prototap.Paths.COMPILED_PROTOS_FILE
import io.spine.tools.prototap.Paths.DESCRIPTOR_SET_FILE
import io.spine.tools.prototap.Paths.TARGET_DIR
import io.spine.tools.prototap.Paths.interimDir
import io.spine.tools.version.Version
import java.io.File
import java.nio.file.Path
import java.util.*
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
        pluginManager.withPlugin("com.google.protobuf") {
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

private val Project.protobufExtension: ProtobufExtension?
    get() = extensions.findByType(ProtobufExtension::class.java)

private fun Project.createProtocPlugin() {
    protobufExtension?.run {
        plugins {
            it.create(PROTOC_PLUGIN_NAME) { locator ->
                locator.artifact = protocPlugin.coordinates
            }
        }
    }
}

/**
 * The Maven artifact of the ProtoTap plugin for `protoc`.
 */
private val protocPlugin: MavenArtifact by lazy {
    MavenArtifact(
        group = "io.spine.tools",
        name = "prototap-protoc-plugin",
        version = io.spine.tools.prototap.gradle.Plugin.readVersion(),
        classifier = PROTOC_PLUGIN_CLASSIFIER,
        extension = "jar"
    )
}

/**
 * Configures [GenerateProtoTask] for the source set specified in the [Extension.sourceSet].
 *
 * The task is configured in the following ways:
 *   1. As an input for `processTaskResources`.
 *      This way all the generated source code becomes resources for the `test` source set.
 *   2. Adds ProtoTap `protoc` plugin.
 *   3. Instructs to generate a descriptor set file, if [Extension.generateDescriptorSet]
 *      property is set to `true`.
 *   4. Declares the tapped files as outputs of the task, so that they are stored in
 *      the build cache and restored on cache hits.
 */
private fun Project.tuneProtoTasks() {
    /* The below block adds a configuration action for the `GenerateProtoTaskCollection`.
       We cannot do it like `generateProtoTasks.all().forEach { ... }` because it
       breaks the configuration order of the `GenerateProtoTaskCollection`.
       This, in turn, leads to missing generated sources in the `compileJava` task. */
    protobufExtension?.generateProtoTasks {
        val sourceSetName = extension.sourceSet.get().name
        it.ofSourceSet(sourceSetName).configureEach { task ->
            task.doFirst {
                task.listCompiledProtoFiles()
            }
            tasks.processTestResources.run {
                copySourcesFrom(task.outputBaseDir)
                copyProtocPluginOutput()
                dependsOn(task)
            }
            task.apply {
                addProtocPlugin()
                grabDescriptorSetFile()
                declareTappedFilesAsOutputs()
            }
        }
    }
}

private fun GenerateProtoTask.listCompiledProtoFiles() {
    val protoFiles = sourceDirs.asFileTree.files.toList().sorted()
    val file = File(project.compiledProtosFile)
    file.parentFile.mkdirs()
    val separator = System.lineSeparator()
    val list = protoFiles.joinToString(separator) { it.path } + separator
    file.writeText(list)
}

private val Project.interimDir: Path
    get() = interimDir(layout.buildDirectory.get().asFile.path)

private fun Project.interimFile(name: String): String =
    interimDir.resolve(name).pathString

private val Project.codeGeneratorRequestFile: String
    get() = interimFile(CODE_GENERATOR_REQUEST_FILE)

private val Project.codeGeneratorRequestJsonFile: String
    get() = interimFile(CODE_GENERATOR_REQUEST_JSON_FILE)

private val Project.descriptorSetFile: String
    get() = interimFile(DESCRIPTOR_SET_FILE)

private val Project.compiledProtosFile: String
    get() = interimFile(COMPILED_PROTOS_FILE)

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

/**
 * Declares the files tapped by ProtoTap as outputs of this task.
 *
 * The `CodeGeneratorRequest` files are written by the ProtoTap `protoc` plugin, and
 * the list of compiled proto files is written by [listCompiledProtoFiles] — both as
 * side effects this task does not know about. Declaring the files as task outputs
 * stores them in the build cache, so that a cache hit on this task restores them
 * along with the generated code. Without the declarations, the tapped files would be
 * missing after a cached build, breaking the test suites which load them from resources.
 *
 * The descriptor set file produced by [grabDescriptorSetFile] needs no declaration here:
 * Protobuf Gradle Plugin already declares its path as a task output.
 *
 * Caching the absolute paths listed in the compiled protos file is safe because
 * the cache key already depends on the project location: the request file path travels
 * base64-encoded as an option of the ProtoTap `protoc` plugin, and plugin options
 * are task inputs. Hence, a cache hit may only occur for the same project directory.
 */
private fun GenerateProtoTask.declareTappedFilesAsOutputs() {
    outputs.file(project.codeGeneratorRequestFile)
        .withPropertyName("codeGeneratorRequestFile")
    outputs.file(project.codeGeneratorRequestJsonFile)
        .withPropertyName("codeGeneratorRequestJsonFile")
    outputs.file(project.compiledProtosFile)
        .withPropertyName("compiledProtosFile")
}

private val TaskContainer.processTestResources: ProcessResources
    get() = named("processTestResources", ProcessResources::class.java).get()

private fun String.base64Encoded(): String {
    val bytes = encodeToByteArray()
    return Base64.getEncoder().encodeToString(bytes)
}
