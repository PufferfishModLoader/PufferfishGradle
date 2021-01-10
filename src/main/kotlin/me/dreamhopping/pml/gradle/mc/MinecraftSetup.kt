package me.dreamhopping.pml.gradle.mc

import kotlinx.coroutines.*
import me.dreamhopping.pml.gradle.data.TargetExt
import me.dreamhopping.pml.gradle.mc.data.manifest.VersionManifest
import me.dreamhopping.pml.gradle.mc.data.version.VersionJson
import me.dreamhopping.pml.gradle.utils.Json
import me.dreamhopping.pml.gradle.utils.http.HttpRequest
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import java.io.File
import java.io.IOException

object MinecraftSetup {
    suspend fun getVersionJson(project: Project, ext: TargetExt, manifest: VersionManifest): VersionJson {
        val entry = manifest.versions.find {
            it.id == ext.version
        } ?: error("Invalid Minecraft version ${ext.version}")

        return withContext(Dispatchers.IO) {
            val versionJson = File(project.gradle.cacheDir, "data/versions/${ext.version}.json")

            try {
                val response = HttpRequest.get(
                    entry.url,
                    "User-Agent" to "PufferfishGradle/1.0"
                )

                response.data.use {
                    if (response.successful) {
                        versionJson.parentFile.mkdirs()
                        versionJson.outputStream().use { output ->
                            it.copyTo(output)
                        }
                    }
                }
            } catch (e: IOException) {
                if (!versionJson.exists()) error("No manifest file found")
            }

            Json.parse<VersionJson>(versionJson.readText())
        }
    }

    @Suppress("RedundantAsync")
    suspend fun setupVersion(
        project: Project,
        ext: TargetExt,
        mainSourceSet: SourceSet,
        config: Configuration,
        manifest: VersionManifest
    ) = coroutineScope {
        val json = getVersionJson(project, ext, manifest)

        val set = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.maybeCreate(
            TargetExt.getMcSourceSetName(ext.version)
        )
        project.dependencies.add(set.implementationConfigurationName, mainSourceSet.runtimeClasspath)

        val libraryConfig = project.configurations.maybeCreate(TargetExt.getMcLibConfigName(ext.version))
        val nativesConfig = project.configurations.maybeCreate(TargetExt.getMcLibNativesConfigName(ext.version))

        LibraryUtil.addLibrariesToConfiguration(project, json.libraries, libraryConfig, nativesConfig)

        libraryConfig.extendsFrom(nativesConfig)
        project.configurations.getByName(set.implementationConfigurationName).extendsFrom(libraryConfig)

        val clientFile = LibraryUtil.getOutputFile(project, "net.minecraft:client:${ext.version}")
        val serverFile = LibraryUtil.getOutputFile(project, "net.minecraft:server:${ext.version}")

        arrayOf(json.downloads.client to clientFile, json.downloads.server to serverFile).map {
            async {
                LibraryUtil.downloadArtifact(it.first, it.second)
            }
        }.awaitAll()

        val serverStrippedFile = LibraryUtil.getOutputFile(project, "net.minecraft:server:${ext.version}", "stripped")

        if (!serverStrippedFile.exists()) {
            println("Stripping server of its libraries")
            JarStripper.strip(serverFile, serverStrippedFile)
        }

        val mergedFile = LibraryUtil.getOutputFile(project, "net.minecraft:joined:${ext.version}", "obfuscated")

        if (!mergedFile.exists()) {
            println("Merging client and server")
            JarMerger.merge(clientFile, serverStrippedFile, mergedFile)
        }

        val classifier = "deobfuscated-${ext.mappings.createId(project, ext.mappingVersion)}"

        val deobfuscatedFile = LibraryUtil.getOutputFile(project, "net.minecraft:joined:${ext.version}-$classifier")

        if (!deobfuscatedFile.exists()) {
            println("Deobfuscating")
            val maps = ext.mappings.load(project, config, ext.mappingVersion)
            JarDeobfuscator.deobf(mergedFile, deobfuscatedFile, maps)
        }

        project.dependencies.add(set.implementationConfigurationName, "net.minecraft:joined:${ext.version}-$classifier")
    }

    val Gradle.repoDir get() = File(cacheDir, "repo")

    val Gradle.cacheDir get() = File(gradleUserHomeDir, "caches/pgmc")
}