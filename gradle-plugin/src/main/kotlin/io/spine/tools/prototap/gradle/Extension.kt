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

import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.prototap.Names.DEFAULT_SOURCE_SET_NAME
import io.spine.tools.prototap.Names.FALLBACK_SOURCE_SET_NAME
import io.spine.tools.prototap.Names.GRADLE_EXTENSION_NAME
import io.spine.tools.prototap.Names.TEST_FIXTURES_PLUGIN_NAME
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.property

/**
 * An extension for a Gradle project which provides options for capturing Protobuf output.
 */
public class Extension(project: Project) {

    /**
     * The `protoc` artifact to be used during for processing proto files.
     *
     * The default value is empty string, which means that ProtoTap assumes that it
     * is used in a project with Protobuf Gradle Plugin fully configured.
     *
     * This property can be used in rare cases when the artifact is not specified directly
     * via the [artifact][com.google.protobuf.gradle.ExecutableLocator.setArtifact] property in
     * the [protobuf/protoc][com.google.protobuf.gradle.ProtobufExtension.protoc] block.
     */
    public val artifact: Property<String> = project.objects.property<String>()
        .convention("")

    /**
     * The source set with proto files for installing the tap.
     *
     * If not specified the Gradle plugin would look for
     * the [testFixtures][DEFAULT_SOURCE_SET_NAME] source set.
     *
     * If it's not available, the plugin would look for
     * the [test][FALLBACK_SOURCE_SET_NAME] source set.
     *
     * If such a source set is not available either, a build time error will occur.
     */
    public val sourceSet: Property<SourceSet> = with(project) {
        objects.property<SourceSet>().convention(
            provider {
                findSourceSet()
            }
        )
    }

    /**
     * Tells if descriptor set file should be captured during the code generation process.
     *
     * The default value is `false`.
     */
    public val generateDescriptorSet: Property<Boolean> =
        project.objects.property<Boolean>().convention(false)
}

private fun Project.findSourceSet(): SourceSet {
    var sourceSet: SourceSet? = sourceSets.findByName(DEFAULT_SOURCE_SET_NAME)
    if (sourceSet == null) {
        sourceSet = sourceSets.findByName(FALLBACK_SOURCE_SET_NAME)
    }
    sourceSet?.let {
        return it
    }
    failWithMissingSourceSetError()
}

private fun Project.failWithMissingSourceSetError(): Nothing {
    // Use vars instead of direct constant references to make the text look closer to final output.
    val protoTap = GRADLE_EXTENSION_NAME
    val testFixtures = DEFAULT_SOURCE_SET_NAME
    val test = FALLBACK_SOURCE_SET_NAME
    val javaTestFixtures = TEST_FIXTURES_PLUGIN_NAME
    val propertyName = "$protoTap.sourceSet"
    error("""
         Unable to figure out a source set to be used by `$protoTap`.
         The project `$name` does not seem to have neither `$testFixtures` nor `$test` source sets.
         A source set is not specified directly via the `$propertyName` either.
         Please consider one of the following:
           1) Adding the `$javaTestFixtures` plugin to your project and using
              the `$testFixtures` source source set for Protobuf types for your tests.
           2) Adding the `$test` source set and putting stub proto files there.
           3) Specifying a custom source set directly via the `$propertyName` property.
        """.trimIndent()
    )
}
