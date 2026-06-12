# Project: ProtoTap

## Overview

ProtoTap is a Gradle plugin which taps the output of the Protobuf compiler
(`protoc`) in the project to which it is applied, placing the generated code,
the `CodeGeneratorRequest`, the descriptor set, and the list of compiled proto
files into `test` resources. Authors of code generators across the Spine SDK
use these captured files to test code generation against real `protoc` output
without custom plumbing.

## Architecture

A Gradle plugin (`gradle-plugin`) cooperating with a companion `protoc` plugin
(`protoc-plugin`) and a small shared API (`api`) which defines the names and
paths of the tapped files. The Gradle plugin configures Protobuf Plugin for
Gradle, registers the ProtoTap `protoc` plugin, copies the tapped files into
test resources via `processTestResources`, and declares them as outputs of the
proto generation task, so that Gradle build-cache hits restore them.

Read [`.agents/guidelines/jvm-project.md`](../.agents/guidelines/jvm-project.md)
for build stack, coding style, tests, and versioning.
