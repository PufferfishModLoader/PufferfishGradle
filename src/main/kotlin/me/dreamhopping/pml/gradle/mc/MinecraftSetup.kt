package me.dreamhopping.pml.gradle.mc

import kotlinx.coroutines.*
import me.dreamhopping.pml.gradle.data.TargetExt
import me.dreamhopping.pml.gradle.mc.data.manifest.VersionManifest
import me.dreamhopping.pml.gradle.mc.data.version.VersionJson
import me.dreamhopping.pml.gradle.utils.Hash.copyFileToMessageDigest
import me.dreamhopping.pml.gradle.utils.Hash.toHexString
import me.dreamhopping.pml.gradle.utils.Json
import me.dreamhopping.pml.gradle.utils.http.HttpRequest
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import java.io.*
import java.security.MessageDigest

object MinecraftSetup {
    private const val START_VERSION = 2

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

        project.configurations.getByName(set.implementationConfigurationName).extendsFrom(libraryConfig, nativesConfig)

        val clientFile = LibraryUtil.getOutputFile(project, "net.minecraft:client:${ext.version}")
        val serverFile = LibraryUtil.getOutputFile(project, "net.minecraft:server:${ext.version}")

        arrayOf(json.downloads.client to clientFile, json.downloads.server to serverFile).map {
            async {
                LibraryUtil.downloadArtifact(it.first, it.second)
            }
        }.awaitAll()

        val serverStrippedFile = LibraryUtil.getOutputFile(project, "net.minecraft:server:${ext.version}", "stripped")
        val serverStrippedFileSha1 = File("${serverStrippedFile.path}.stateHash")

        if (!serverStrippedFile.exists() || !serverStrippedFileSha1.exists() || multiFileSha1(
                serverFile,
                serverStrippedFile
            ) != serverStrippedFileSha1.readText()
        ) {
            println("Stripping server of its libraries")
            serverStrippedFileSha1.delete()
            JarStripper.strip(serverFile, serverStrippedFile)
            serverStrippedFileSha1.writeText(multiFileSha1(serverFile, serverStrippedFile))
        }

        val mergedFile = LibraryUtil.getOutputFile(project, "net.minecraft:joined:${ext.version}", "obfuscated")
        val mergedFileSha1 = File("${mergedFile.path}.stateHash")

        if (!mergedFile.exists() || !mergedFileSha1.exists() || multiFileSha1(
                clientFile,
                serverStrippedFile,
                mergedFile,
                extra = START_VERSION
            ) != mergedFileSha1.readText()
        ) {
            println("Merging client and server")
            mergedFileSha1.delete()

            JarMerger.merge(clientFile, serverStrippedFile, mergedFile)

            mergedFileSha1.writeText(multiFileSha1(clientFile, serverStrippedFile, mergedFile, extra = START_VERSION))
        }

        val classifier = "deobfuscated-${ext.mappings.createId(project, ext.mappingVersion)}"

        val deobfuscatedFile = LibraryUtil.getOutputFile(project, "net.minecraft:joined:${ext.version}", classifier)
        val deobfuscatedFileSha1 = File("${deobfuscatedFile.path}.stateHash")

        val maps = ext.mappings.load(project, config, ext.mappingVersion)

        if (!deobfuscatedFile.exists() || !deobfuscatedFileSha1.exists() || multiFileSha1(
                mergedFile,
                deobfuscatedFile,
                extra = maps
            ) != deobfuscatedFileSha1.readText()
        ) {
            println("Deobfuscating")
            deobfuscatedFileSha1.delete()

            JarDeobfuscator.deobf(mergedFile, deobfuscatedFile, maps)

            deobfuscatedFileSha1.writeText(multiFileSha1(mergedFile, deobfuscatedFile, extra = maps))
        }

        project.dependencies.add(set.implementationConfigurationName, "net.minecraft:joined:${ext.version}:$classifier")
    }

    val Gradle.repoDir get() = File(cacheDir, "repo")

    val Gradle.cacheDir get() = File(gradleUserHomeDir, "caches/pgmc")

    private fun multiFileSha1(vararg files: File, extra: Serializable? = null): String {
        val digest = MessageDigest.getInstance("SHA-1")
        files.forEach {
            digest.copyFileToMessageDigest(it)
        }
        digest.update((if (extra != null) 1 else 0).toByte())
        if (extra != 0) {
            val out = ByteArrayOutputStream()
            val obj = ObjectOutputStream(out)
            obj.writeObject(extra)
            digest.update(out.toByteArray())
        }
        return digest.digest().toHexString()
    }
}