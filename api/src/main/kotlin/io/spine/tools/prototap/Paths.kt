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

package io.spine.tools.prototap

import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString

/**
 * Constants and utility functions for calculating directory and file names
 * used by ProtoTap Gradle and `protoc` plugins.
 *
 * This object may also be useful for users of ProtoTap to obtain its output.
 */
public object Paths {

    /**
     * The name of the subdirectory for collecting files.
     */
    public const val TARGET_DIR: String = PROTOC_PLUGIN_NAME

    /**
     * The simple name of the `CodeGeneratorRequest` class.
     */
    private const val SIMPLE_CLASS_NAME = "CodeGeneratorRequest"

    /**
     * The name of the file containing `CodeGeneratorRequest` message obtained
     * by the ProtoTap `protoc` plugins.
     */
    public const val CODE_GENERATOR_REQUEST_FILE: String = "$SIMPLE_CLASS_NAME.binpb"

    /**
     * The name of the JSON version of the `CodeGeneratorRequest` message created
     * in the same directory with the binary version of the file for debug purposes.
     */
    public const val CODE_GENERATOR_REQUEST_JSON_FILE: String = "$SIMPLE_CLASS_NAME.pb.json"

    /**
     * The name of the descriptor set file obtained by the ProtoTap Gradle plugin.
     */
    public const val DESCRIPTOR_SET_FILE: String = "FileDescriptorSet.binpb"

    /**
     * The list of full file names compiled by `protoc`.
     */
    public const val COMPILED_PROTOS_FILE: String = "CompiledProtoFiles.txt"

    /**
     * Obtains the path to the intermediate directory for storing some of the intercepted
     * files before they are copied to the [outputRoot] directory.
     *
     * @param buildDir The path to the `build` directory of the project.
     */
    public fun interimDir(buildDir: String): Path =
        Paths.get("$buildDir/$TARGET_DIR")

    /**
     * Obtains the path to the root directory into which ProtoTap puts all the intercepted files.
     *
     * By convention, it is a subdirectory named [TARGET_DIR] placed under
     * `$buildDir/resources/test` of a Gradle project.
     *
     * @param buildDir The path to the `build` directory of the project.
     */
    public fun outputRoot(buildDir: String): Path =
        Paths.get("$buildDir/resources/test/$TARGET_DIR")

    /**
     * Obtains a full path to the file with given [shortFileName] under
     * the given [buildDir] of a Gradle project.
     *
     * By convention, the file is placed under the [TARGET_DIR] directory,
     * which is created under `$buildDir/resources/test/`.
     *
     * @param buildDir The path to the `build` directory of the project.
     * @param shortFileName The name of the file under the [outputRoot] directory.
     */
    public fun outputFile(buildDir: String, shortFileName: String): String =
        outputRoot(buildDir).resolve(shortFileName).pathString
}
