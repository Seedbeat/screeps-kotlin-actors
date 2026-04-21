import groovy.json.JsonOutput
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

plugins {
    kotlin("multiplatform") version "2.3.10"
    kotlin("plugin.js-plain-objects") version "2.3.10"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
}

val screepsUser: String? by project
val screepsPassword: String? by project
val screepsToken: String? by project
val screepsHost: String? by project
val screepsBranch: String? by project
val screepsDebugBranch: String? by project
val bundleMode: String? by project
val branch = screepsBranch ?: "default"
val debugBranch = screepsDebugBranch ?: "test"
val host = screepsHost ?: "https://screeps.com"

val bundledDirectory: Directory = project.layout.buildDirectory.dir("bundle").get()
val bundledJsDirectory: Directory = bundledDirectory.dir("js")
val releaseDirectory: Directory = bundledDirectory.dir("release")
val debugReleaseDirectory: Directory = bundledDirectory.dir("debug-release")

val webpackJsFilename: String = "${project.name}-webpack.js"
val optimizedJsFilename: String = "${project.name}-optimized.js"
val releaseJsFilename: String = "${project.name}.js"

val isBundleDebug: Boolean
    get() = when (bundleMode?.lowercase()) {
        null, "", "debug" -> true
        "production" -> false
        else -> throw GradleException(
            "Unknown bundleMode='$bundleMode'. Use 'debug' or 'production'"
        )
    }

kotlin {
    js {
        binaries.executable()
        useCommonJs()

        if (isBundleDebug) {
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions.freeCompilerArgs.add("-Xir-minimized-member-names=false")
                }
            }
        }

        browser {
            commonWebpackConfig {
                output?.globalObject = "this"
                outputPath = bundledJsDirectory.asFile
                outputFileName = webpackJsFilename

                mode = if (isBundleDebug) {
                    KotlinWebpackConfig.Mode.DEVELOPMENT
                } else {
                    KotlinWebpackConfig.Mode.PRODUCTION
                }
            }
        }
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation("io.github.exav:screeps-kotlin-types:2.2.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation(devNpm("google-closure-compiler", "20250701.0.0"))
            }
        }
    }
}

val optimize by tasks.registering(Exec::class) {
    group = "build"
    description = "Optimizes the Kotlin/JS bundle with Closure Compiler."
    dependsOn("build")

    onlyIf { !isBundleDebug }

    val minifiedJsFile = bundledJsDirectory.file(webpackJsFilename)
    val optimizedJsFile = bundledJsDirectory.file(optimizedJsFilename)
    val closureCli = project.layout.buildDirectory.file("js/node_modules/google-closure-compiler/cli.js")

    inputs.file(minifiedJsFile)
    inputs.file(closureCli)
    outputs.file(optimizedJsFile)

    executable("node")

    args(
        closureCli.get().asFile.absolutePath,
        "--js=${minifiedJsFile.asFile.absolutePath}",
        "--js_output_file=${optimizedJsFile.asFile.absolutePath}",
        "-O=SIMPLE",
        "--env=BROWSER",
        "--warning_level=QUIET"
    )
}

val releaseMinified = tasks.register<Sync>("release-minified") {
    group = "screeps"
    description = "Copies the webpack bundle to the Screeps release module."
    dependsOn("build")

    val minifiedJsFile = bundledJsDirectory.file(webpackJsFilename)

    from(minifiedJsFile) {
        rename { _: String -> releaseJsFilename }
    }
    into(releaseDirectory)
}

val releaseOptimized = tasks.register<Sync>("release-optimized") {
    group = "screeps"
    description = "Copies the Closure-optimized bundle to the Screeps release module."
    dependsOn(optimize)

    val optimizedJsFile = bundledJsDirectory.file(optimizedJsFilename)

    from(optimizedJsFile) {
        rename { _: String -> releaseJsFilename }
    }
    into(releaseDirectory)
}

val release = tasks.register("release") {
    group = "screeps"
    description = "Builds the default optimized Screeps release bundle."

    if (isBundleDebug) {
        dependsOn(releaseMinified)
    } else {
        dependsOn(releaseOptimized)
    }
}

tasks.register("deploy") {
    group = "screeps"
    description = "Uploads the optimized release bundle to the configured Screeps server."
    dependsOn(release)

    configureScreepsUpload(releaseDirectory, branch)
}

fun Task.configureScreepsUpload(codeDirectory: Directory, targetBranch: String) {
    doLast {
        if (screepsToken == null && (screepsUser == null || screepsPassword == null)) {
            throw InvalidUserDataException("you need to supply either screepsUser and screepsPassword or screepsToken before you can upload code")
        }
        val codeLocation = codeDirectory.asFile
        if (!codeLocation.isDirectory) {
            throw InvalidUserDataException("found no code to upload at ${codeLocation.path}")
        }

        // https://docs.screeps.com/commit.html#Using-direct-API-access
        fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))
        fun File.encodeBase64(): String = Base64.getEncoder().encodeToString(this.readBytes())

        fun getFiles(extension: String): Pair<File?, List<File>> =
            codeLocation
                .listFiles { _, name -> name.endsWith(".$extension") }.orEmpty()
                .sortedBy { it.name }
                .partition { it.nameWithoutExtension == project.name }
                .let { Pair(it.first.singleOrNull(), it.second) }

        val (mainJsModule, otherJsModules) = getFiles("js")

        val main = mainJsModule ?: throw IllegalStateException(buildString {
            append("Could not find js file corresponding to main module in ${codeLocation.absolutePath}. ")
            append("Was looking for ${project.name}.js")
        })

        val modules = mutableMapOf<String, Any>()

        fun addModule(name: String, file: File, isBinary: Boolean) {
            if (isBinary) {
                modules[name] = mapOf("binary" to file.encodeBase64())
            } else {
                modules[name] = file.readText()
            }
        }

        addModule("main", main, isBinary = false)
        otherJsModules.forEach { addModule(it.nameWithoutExtension, it, isBinary = false) }

        val uploadContent = mapOf(
            "branch" to targetBranch,
            "modules" to modules
        )
        val uploadContentJson = JsonOutput.toJson(uploadContent)

        logger.lifecycle("Uploading ${modules.keys.count()} files to branch '$targetBranch' on server $host")
        logger.debug("Request Body: $uploadContentJson")


        val url = URI("$host/api/user/code")
        val request = HttpRequest.newBuilder()
            .uri(url)
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(uploadContentJson))

        if (screepsToken != null) {
            request.header("X-Token", screepsToken)
        } else {
            request.header("Authorization", "Basic " + "$screepsUser:$screepsPassword".encodeBase64())
        }

        val response = HttpClient.newBuilder()
            .connectTimeout(30.seconds.toJavaDuration())
            .build()
            .send(request.build(), HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() in 200..299) {
            logger.lifecycle("Upload done! ${response.body()}")
        } else {
            throw GradleException("Upload failed! ${response.statusCode()} ${response.body()}")
        }
    }
}
