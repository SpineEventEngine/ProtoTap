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

import io.spine.io.Resource
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import io.spine.tools.prototap.Paths.COMPILED_PROTOS_FILE

/**
 * Utility class for working with a [COMPILED_PROTOS_FILE] created by ProtoTap.
 *
 * @param classLoader The classloader of the classes in resources of which ProtoTap
 *   creates the [COMPILED_PROTOS_FILE].
 */
public class CompiledProtosFile(classLoader: ClassLoader) {

    private val fileList: Resource by lazy {
        Resource.file("$PROTOC_PLUGIN_NAME/$COMPILED_PROTOS_FILE", classLoader)
    }

    /**
     * Verifies if the resource file with the list of compiled porto files exists.
     */
    public fun exists(): Boolean = fileList.exists()

    /**
     * Read the lines from the file, filtering out blank ones.
     */
    public fun list(): List<String> =
        fileList.read()
            .lines()
            .filter { it.isNotBlank() }

    /**
     * Creates a list of files of the type [F] by applying
     * the [block] for each line of the file.
     */
    public fun <F : Any> listFiles(block: (String) -> F): List<F> =
        list().map(block)
}
