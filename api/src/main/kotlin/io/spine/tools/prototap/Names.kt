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

package io.spine.tools.prototap

/**
 * Names for the resources ProtoTap produces.
 */
public object Names {

    /**
     * The ID of the ProtoTap Gradle Plugin.
     */
    public const val GRADLE_PLUGIN_ID: String = "io.spine.prototap"

    /**
     * The name of a Gradle project extension added by ProtoTap Gradle Plugin.
     */
    public const val GRADLE_EXTENSION_NAME: String = "protoTap"

    /**
     * The name ProtoTap uses when passing itself to `protoc` compiler.
     */
    public const val PROTOC_PLUGIN_NAME: String = "prototap"

    /**
     * The name of the source set which is used by default for tapping Protobuf compiler.
     *
     * This constant is used if the `sourceSet` property of the `protoTap` project extension
     * added by the ProtoTap Gradle Plugin is not specified.
     *
     * If a project does not apply the `java-test-fixtures` Gradle plugin, then
     * the [`test`][FALLBACK_SOURCE_SET_NAME] source set conventionally will be used.
     */
    public const val DEFAULT_SOURCE_SET_NAME: String = "testFixtures"

    /**
     * The name of the source set to be used for tapping Protobuf compiler, if
     * the project does not have a source set with the [DEFAULT_SOURCE_SET_NAME].
     *
     * If the project does not have the [`test`][FALLBACK_SOURCE_SET_NAME] source
     * set either, a build time error will occur.
     */
    public const val FALLBACK_SOURCE_SET_NAME: String = "test"

    /**
     * The classifier used for the executable fat JAR of the ProtoTap `protoc` plugin archive.
     */
    public const val PROTOC_PLUGIN_CLASSIFIER: String = "exe"

    /**
     * The name of the `java-test-fixtures` plugin.
     */
    public const val TEST_FIXTURES_PLUGIN_NAME: String = "java-test-fixtures"
}
