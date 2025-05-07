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

import com.google.protobuf.gradle.protobuf
import io.spine.dependency.lib.GoogleApis
import io.spine.dependency.test.JUnit
import io.spine.dependency.lib.Protobuf
import io.spine.gradle.repo.standardToSpineSdk

buildscript {
    standardSpineSdkRepositories()
}

plugins {
    java
    id("com.google.protobuf")
    id("@PROTOTAP_PLUGIN_ID@") version "@PROTOTAP_VERSION@"
}

repositories {
    mavenLocal()
    standardToSpineSdk()
}

protobuf {
    protoc {
        artifact = io.spine.dependency.lib.Protobuf.compiler
    }
}

@Suppress(
    "UnstableApiUsage" /* testing suites feature */
)
testing {
    suites {
        val functionalTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(JUnit.version)
            dependencies {
                implementation(Protobuf.javaLib)
                implementation(GoogleApis.commonProtos)
            }
        }
    }
}

val functionalTest: SourceSet by project.sourceSets.getting

prototap {
    sourceSet.set(functionalTest)
    generateDescriptorSet.set(true)
}

// Force Gradle to execute the `generateFunctionalTestProto` task, which otherwise
// "hang in the air" without dependencies because we don't have `main` and `test` source sets.
val generateFunctionalTestProto by tasks.getting
tasks.check {
    dependsOn(generateFunctionalTestProto)
}
