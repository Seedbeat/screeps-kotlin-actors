# Build And Deploy

Last source scan: 2026-04-21.

This document describes the Gradle/Kotlin JS build and Screeps upload pipeline.

## Toolchain

Gradle files:

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradlew`
- `gradlew.bat`
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

- webpack output: `build/bundle/js/screeps-kotlin-actors-webpack.js`
- optimized output: `build/bundle/js/screeps-kotlin-actors-optimized.js`
- release output: `build/bundle/release/screeps-kotlin-actors.js`

Bundle mode defaults to `debug`.

Modes:

- `debug`: webpack uses `KotlinWebpackConfig.Mode.DEVELOPMENT`, Kotlin/JS compilation adds
  `-Xir-minimized-member-names=false`, `optimize` is skipped, and `release` copies the webpack bundle directly.
- `production`: webpack uses `KotlinWebpackConfig.Mode.PRODUCTION`, `optimize` runs Closure Compiler, and `release`
  copies the optimized bundle.

## Important Tasks

Basic Kotlin/JS build:

```text
./gradlew build
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.

Bundle mode switch:

```text
./gradlew release -PbundleMode=production
```

Supported values are `debug` and `production`.

Closure Compiler optimization:

```text
./gradlew optimize
```

This task depends on `build`, runs only when `bundleMode=production`, and invokes `node` on the Closure Compiler CLI
with:

- `-O=SIMPLE`
- `--env=BROWSER`
- quiet warnings

Gradle tracks the webpack bundle, Closure CLI, and optimized output for this task.

Release bundle:

```text
./gradlew release
```

This task syncs `build/bundle/release/screeps-kotlin-actors.js` from either the webpack bundle or the Closure-optimized
bundle, depending on `bundleMode`.

The actual `release` dependency is selected during Gradle configuration:

- default `bundleMode=debug`: `release` depends on `release-minified`
- `bundleMode=production`: `release` depends on `release-optimized`

Alternative release tasks:

- `./gradlew release-optimized`: sync the Closure-optimized bundle to the release directory.
- `./gradlew release-minified`: sync the webpack bundle directly to the release directory without running Closure
  Compiler.

Use `release` rather than calling `release-optimized` directly unless you intentionally want to bypass the mode-selected
default. In debug mode, `optimize` is skipped.

Deploy:

```text
./gradlew deploy
```

This task depends on `release` and uploads the release directory contents to the Screeps API.

Deploy fails the Gradle task when the Screeps API returns a non-2xx response.
The HTTP client uses a 30 second connect timeout.

Local deploy:

```text
./gradlew deploy-local
```

This task depends on `release` and copies the release directory contents to `screepsLocal/screepsBranch`. The generated
release module is renamed to `main.js` for the local Screeps directory. The task overwrites matching generated modules
but does not delete other files in the local branch directory.

## Deploy Properties

Supported Gradle properties:

- `screepsUser`
- `screepsPassword`
- `screepsToken`
- `screepsHost`
- `screepsLocal`
- `screepsBranch`
- `bundleMode`

Defaults:

- `screepsHost`: `https://screeps.com`
- `screepsBranch`: `default`
- `bundleMode`: `debug`

`screepsLocal` has no default. Set it to the local Screeps scripts root; `deploy-local` appends `screepsBranch` to that
path.

`build.gradle.kts` currently also declares `screepsDebugBranch` and `debugReleaseDirectory`, but no registered task uses
them. There is no `release-debug` or `deploy-debug` task in the current script.

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
- production release changes: `./gradlew release -PbundleMode=production`
- deploy changes: `./gradlew deploy` only when credentials and target branch are intentional
- local deploy changes: `./gradlew deploy-local` only when `screepsLocal` and target branch are intentional

The network can be restricted in automation environments, so Gradle may fail if dependencies are not already cached.

## Artifact Stability

Screeps upload depends on artifact names and the exported `loop()` function. Do not rename release artifacts, module
names, or the exported entrypoint without documenting the deploy impact.
