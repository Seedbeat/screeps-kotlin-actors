# Build And Deploy

Last source scan: 2026-04-21.

This document describes the Gradle/Kotlin JS build and Screeps upload pipeline.

## Toolchain

Gradle files:

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/wrapper/*`

Project name:

```text
screeps-kotlin-actors
```

Kotlin plugins:

- `kotlin("multiplatform") version "2.3.10"`
- `kotlin("plugin.js-plain-objects") version "2.3.10"`

Kotlin target:

- JS
- executable binary
- CommonJS output
- browser webpack config

Repositories:

- Maven Central
- JetBrains Kotlin dev repository
- JetBrains wasm experimental repository

Dependencies:

- `io.github.exav:screeps-kotlin-types:2.2.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2`
- dev npm `google-closure-compiler:20250701.0.0`

## Build Outputs

Configured bundle directories:

- minified webpack output: `build/bundle/js/screeps-kotlin-actors-minified.js`
- optimized output: `build/bundle/js/screeps-kotlin-actors-optimized.js`
- release output: `build/bundle/release/screeps-kotlin-actors.js`

The webpack mode is currently `DEVELOPMENT`. Production mode is present as a commented option in `build.gradle.kts`.

## Important Tasks

Basic Kotlin/JS build:

```text
./gradlew build
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.

Closure Compiler optimization:

```text
./gradlew optimize
```

This task depends on `build` and invokes `node` on the Closure Compiler CLI with:

- `-O=SIMPLE`
- `--env=BROWSER`
- quiet warnings

Release bundle:

```text
./gradlew release
```

This task depends on `optimize`, clears the release directory, and copies the optimized JS file to
`screeps-kotlin-actors.js`.

Deploy:

```text
./gradlew deploy
```

This task depends on `release` and uploads the release directory contents to the Screeps API.

## Deploy Properties

Supported Gradle properties:

- `screepsUser`
- `screepsPassword`
- `screepsToken`
- `screepsHost`
- `screepsBranch`

Defaults:

- `screepsHost`: `https://screeps.com`
- `screepsBranch`: `test`

Authentication:

- if `screepsToken` exists, deploy uses `X-Token`
- otherwise deploy uses basic auth from user and password

`gradle.properties` is ignored by git and may contain local secrets. Do not commit or quote token values.

## Upload Format

Deploy sends a JSON body matching the Screeps direct API format:

```json
{
  "branch": "<branch-name>",
  "modules": {
    "main": "<main JS module text>"
  }
}
```

The release file named after the project becomes the Screeps `main` module. Other JS files in the release directory are
uploaded as additional modules.

## Verification

There are no test source files in this checkout at the time of this scan.

Use these checks by change type:

- docs-only: `git diff --check`
- source changes: `./gradlew build`
- release changes: `./gradlew release`
- deploy changes: `./gradlew deploy` only when credentials and target branch are intentional

The network can be restricted in automation environments, so Gradle may fail if dependencies are not already cached.

## Artifact Stability

Screeps upload depends on artifact names and the exported `loop()` function. Do not rename release artifacts, module
names, or the exported entrypoint without documenting the deploy impact.
