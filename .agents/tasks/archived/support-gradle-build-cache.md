# Support Gradle build cache

## Problem

`GenerateProtoTask` (protobuf-gradle-plugin 0.10.0) is `@CacheableTask`.
ProtoTap makes that task produce three files under the interim directory
`$buildDir/prototap/` without declaring them as task outputs:

1. `CodeGeneratorRequest.binpb` — written by the ProtoTap `protoc` plugin
   process; the target path travels base64-encoded as a plugin option
   (tracked as `@Input` only).
2. `CodeGeneratorRequest.pb.json` — written next to the binary file by
   `CodeGeneratorRequestWriter.writeJson()`.
3. `CompiledProtoFiles.txt` — written by the `doFirst` action added in
   `tuneProtoTasks` (`listCompiledProtoFiles`).

On a build-cache hit the task actions and `protoc` do not run, and Gradle
restores only the declared outputs (`outputBaseDirProperty`, plus the
descriptor-set file that protobuf-gradle-plugin already declares via
`@Nested getDescriptorSetOptionsForCaching()` → `@OutputFile path`).
The interim directory stays empty, `processTestResources` copies nothing
from it, and consumers fail, e.g. `PluginTestSetup` in `core-jvm-compiler`
`*-tests` modules:

```
Unable to find `prototap/CodeGeneratorRequest.binpb`
```

Observed on 2026-06-11: `:annotation-tests:clean :annotation-tests:build`
with the build cache on failed all specs; `--no-build-cache` passed.
See `core-jvm-compiler/.agents/memory/project/prototap-build-cache.md`.

## Fix

In `gradle-plugin/.../Plugin.kt`, register the three files as outputs of
the configured `GenerateProtoTask` (`outputs.file(...)` with property
names). The descriptor-set file needs no declaration — protobuf-gradle-plugin
already declares it; double-declaring would overlap.

Notes:

- Cache relocatability is naturally limited: the absolute capture path is
  a protoc-plugin option and thus part of the cache key. Same-path rebuilds
  (the `clean build` case) hit the cache and now restore the capture.
- `sourceDirs` is `@SkipWhenEmpty`, so the no-protos case never caches.

## Test

New test-resources project `build-cache` (clone of `default-values` with a
project-local `buildCache.local.directory` in `settings.gradle.kts`), plus
a `PluginSpec` case that:

1. runs `build` with `--build-cache`;
2. runs `clean`;
3. runs `build` again and asserts `:generateTestFixturesProto` is
   `FROM_CACHE` and the tapped files exist under
   `build/resources/test/prototap/`.

The case must fail before the fix and pass after.

## Verification

- New test red on unfixed plugin, green after the fix.
- `./gradlew build dokkaGenerate` passes.
- Reviewers per pre-PR checklist.

## Status

- [x] Plan written
- [x] Fix in `Plugin.kt`
- [x] Functional test (red before the fix, green after)
- [x] Verification (full `build dokkaGenerate` passed; reviewers'
      findings applied)
