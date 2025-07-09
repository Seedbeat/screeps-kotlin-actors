import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

plugins {
    kotlin("multiplatform") version "2.2.0"
    kotlin("plugin.js-plain-objects") version "2.2.0"
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
val branch = screepsBranch ?: "test"
val host = screepsHost ?: "https://screeps.com"
val bundledJsDirectory: Directory = project.layout.buildDirectory.dir("bundle").get()
val minifiedJsFilename: String = "${project.name}-minified.js"
val optimizedJsFilename: String = "${project.name}-optimized.js"
val releaseJsDirectory: Directory = bundledJsDirectory.dir("release")
val releaseJsFilename: String = "${project.name}.js"

val minifiedWasmDirectory: Directory = project.layout.buildDirectory.dir("minified-wasm").get()
val minifiedWasmFilename: String = "${project.name}-wasm.js"

kotlin {
    js {
        binaries.executable()
        useCommonJs()

        browser {
            commonWebpackConfig {
                output?.globalObject = "this"
                outputPath = bundledJsDirectory.asFile
                outputFileName = minifiedJsFilename
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    wasmJs {
        binaries.executable()
        useCommonJs()

        browser {
            commonWebpackConfig {
                output?.globalObject = "this"
                outputPath = minifiedWasmDirectory.asFile
                outputFileName = minifiedWasmFilename
            }
        }
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation("io.github.exav:screeps-kotlin-types:2.1.0")
                implementation(devNpm("google-closure-compiler", "20250701.0.0"))
            }
        }

        val wasmJsMain by getting {
            dependencies {
                sourceSets.jsMain
            }
        }
    }
}

tasks.register<Exec>("optimize", Exec::class) {
    group = "build"
    dependsOn(tasks["build"])

    val minifiedFile = bundledJsDirectory.file(minifiedJsFilename).asFile
    val optimizedFile = bundledJsDirectory.file(optimizedJsFilename).asFile
    val releaseFile = releaseJsDirectory.file(releaseJsFilename).asFile

    val closureCli = project.layout.buildDirectory.file("js/node_modules/google-closure-compiler/cli.js").get().asFile

    executable("node")
    args(
        "$closureCli",
        "--js=${minifiedFile}",
        "--js_output_file=${optimizedFile}",
        "-O=SIMPLE",
        "--env=BROWSER",
        "--warning_level=QUIET"
    )

    doLast {
        releaseFile.delete()
        optimizedFile.copyTo(releaseFile)
    }
}

tasks.register("deploy") {
    group = "screeps"
    dependsOn(tasks["optimize"])

    doFirst { // use doFirst to avoid running this code in configuration phase
        if (screepsToken == null && (screepsUser == null || screepsPassword == null)) {
            throw InvalidUserDataException("you need to supply either screepsUser and screepsPassword or screepsToken before you can upload code")
        }
        val minifiedCodeLocation = releaseJsDirectory.asFile
        if (!minifiedCodeLocation.isDirectory) {
            throw InvalidUserDataException("found no code to upload at ${minifiedCodeLocation.path}")
        }

        /*
        The screeps server expects us to upload our code in the following json format
        https://docs.screeps.com/commit.html#Using-direct-API-access
        {
            "branch":"<branch-name>"
            "modules": {
                "main":<main script as a string, must contain the "loop" function>
                "module1":<a module that is imported in the main script>
            }
        }
        The following code extracts the generated js code from the build folder and writes it to a string that has the
        correct format
         */

        val jsFiles = minifiedCodeLocation.listFiles { _, name -> name.endsWith(".js") }.orEmpty()
        val (mainModule, otherModules) = jsFiles.partition { it.nameWithoutExtension == project.name }
        val main = mainModule.firstOrNull()
            ?: throw IllegalStateException("Could not find js file corresponding to main module in ${minifiedCodeLocation.absolutePath}. Was looking for ${project.name}.js")

        val modules = mutableMapOf<String, String>()
        modules["main"] = main.readText()
        modules.putAll(otherModules.associate { it.nameWithoutExtension to it.readText() })

        val uploadContent = mapOf(
            "branch" to branch,
            "modules" to modules
        )
        val uploadContentJson = groovy.json.JsonOutput.toJson(uploadContent)

        logger.lifecycle("Uploading ${jsFiles.count()} files to branch '$branch' on server $host")
        logger.debug("Request Body: $uploadContentJson")

        // upload using java 11 http client -> requires java 11
        val url = URI("$host/api/user/code")
        val request = HttpRequest.newBuilder()
            .uri(url)
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(uploadContentJson))

        if (screepsToken != null) {
            request.header("X-Token", screepsToken)
        } else {
            fun String.encodeBase64() = Base64.getEncoder().encodeToString(this.toByteArray())
            request.header("Authorization", "Basic " + "$screepsUser:$screepsPassword".encodeBase64())
        }

        val response = HttpClient.newBuilder()
            .build()
            .send(request.build(), HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() in 200..299) {
            logger.lifecycle("Upload done! ${response.body()}")
        } else {
            val shortMessage = "Upload failed! ${response.statusCode()}"

            logger.lifecycle(shortMessage)
            logger.lifecycle(response.body())
            logger.error(shortMessage)
            logger.error(response.body())
        }

    }

}
