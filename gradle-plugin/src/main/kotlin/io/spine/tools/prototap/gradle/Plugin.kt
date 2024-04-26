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

package io.spine.tools.prototap.gradle

import com.google.protobuf.gradle.GenerateProtoTask
import io.spine.tools.code.manifest.Version
import io.spine.tools.gradle.Artifact
import io.spine.tools.gradle.artifact
import io.spine.tools.gradle.protobuf.ProtobufDependencies
import io.spine.tools.gradle.protobuf.protobufExtension
import io.spine.tools.prototap.Names.GRADLE_EXTENSION_NAME
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_CLASSIFIER
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy.INCLUDE
import com.google.protobuf.gradle.id
import io.spine.tools.prototap.Names.DESCRIPTOR_SET_FILE
import org.gradle.api.tasks.TaskContainer
import org.gradle.language.jvm.tasks.ProcessResources

public class Plugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        createExtension()
        pluginManager.withPlugin(ProtobufDependencies.gradlePlugin.id) {
            tapProtobuf()
        }
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
    setProtocArtifact()
    createProtocPlugin()
    tuneProtoTasks()
}

/**
 * Obtains the extension our plugin added to this Gradle project.
 */
private val Project.extension: Extension
    get() = extensions.getByType(Extension::class.java)

private fun Project.setProtocArtifact() {
    protobufExtension?.protoc {
        val artifact = extension.artifact.get()
        if (artifact.isNotBlank()) {
            it.artifact = artifact
        }
    }
}

private fun Project.createProtocPlugin() {
    protobufExtension?.plugins {
        it.create(PROTOC_PLUGIN_NAME) { locator ->
            locator.artifact = protocPlugin.notation()
        }
    }
}

private val protoTapVersion: String by lazy {
    Version.fromManifestOf(Plugin::class.java).value
}

private val protocPlugin: Artifact by lazy {
    artifact {
        useSpineToolsGroup()
        name = "prototap-protoc-plugin"
        version = protoTapVersion
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
        it.ofSourceSet(sourceSetName).forEach { task ->
            task.apply {
                collectGeneratedJavaCode()
                addProtocPlugin()
                grabDescriptorSetFile()
            }
        }
    }
}

private fun GenerateProtoTask.collectGeneratedJavaCode() {
    project.tasks.processTaskResources.apply {
        from(outputs)
        duplicatesStrategy = INCLUDE
    }
}

private fun GenerateProtoTask.addProtocPlugin() {
    plugins.apply {
        id(PROTOC_PLUGIN_NAME)
    }
}

private fun GenerateProtoTask.grabDescriptorSetFile() {
    if (project.extension.generateDescriptorSet.get()) {
        generateDescriptorSet = true
        descriptorSetOptions.apply {
            path = "${project.buildDir}/resources/test/$PROTOC_PLUGIN_NAME/$DESCRIPTOR_SET_FILE"
            includeSourceInfo = true
            includeImports = true
        }
    }
}

private val TaskContainer.processTaskResources: ProcessResources
    get() = named("processTestResources", ProcessResources::class.java).get()
