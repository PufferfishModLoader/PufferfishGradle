package me.dreamhopping.pml.gradle.target

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import me.dreamhopping.pml.gradle.data.minecraft.VersionJson
import me.dreamhopping.pml.gradle.tasks.download.DownloadTask
import me.dreamhopping.pml.gradle.user.TargetData
import me.dreamhopping.pml.gradle.user.UserData
import me.dreamhopping.pml.gradle.util.*
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import java.io.File

object TargetConfigurator {
    private const val VERSION_MANIFEST_PATH = "versions/manifest.json"
    private const val VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"

    fun configureTarget(project: Project, target: TargetData, parent: UserData, addDefaultMaps: Boolean) {
        val versionJson = project.dataFile(target.versionJsonPath)
        if (!versionJson.exists()) {
            download(project.getVersionJsonUrl(target.version), versionJson)
        }
        val version = try {
            versionJson.fromJson<VersionJson>()
        } catch (ex: JsonParseException) {
            // Version JSON is corrupted - redownload and try again.
            download(project.getVersionJsonUrl(target.version), versionJson, ignoreInitialState = true)
            versionJson.fromJson<VersionJson>()
        }

        assert(version.id == target.version)

        val minecraftConfiguration = project.configurations.maybeCreate(target.mcConfigName)
        val minecraftLibrariesConfiguration = project.configurations.maybeCreate(target.mcLibsConfigName)
        val minecraftNativeLibrariesConfiguration = project.configurations.maybeCreate(target.mcNativeLibsConfigName)

        minecraftLibrariesConfiguration.extendsFrom(minecraftNativeLibrariesConfiguration)
        minecraftConfiguration.extendsFrom(minecraftLibrariesConfiguration)

       project.tasks.register(target.downloadClientName, DownloadTask::class.java) {
            it.url = version.downloads.client.url
            it.sha1 = version.downloads.client.sha1
            it.output = project.repoFile("net.minecraft", "client", target.version)
        }

        version.downloads.server?.let {
            project.tasks.register(target.downloadServerName, DownloadTask::class.java) { task ->
                task.url = it.url
                task.sha1 = it.sha1
                task.output = project.repoFile("net.minecraft", "server", target.version)
            }
        }

        project.afterEvaluate {


            val implementation = parent.mainSourceSet.implementationConfigurationName
            project.configurations.getByName(implementation).extendsFrom(minecraftConfiguration)
        }
    }

    fun setUpJarTasks(project: Project, target: TargetData) {
        val set = project.java.sourceSets.maybeCreate(target.sourceSetName)
        project.tasks.register(set.jarTaskName, Jar::class.java) {
            it.dependsOn(set.compileJavaTaskName, set.processResourcesTaskName)
            it.from(set.output)
            it.archiveClassifier.set(set.name)
        }
    }

    private fun Project.getVersionJsonUrl(version: String): String {
        val manifestFile = dataFile(VERSION_MANIFEST_PATH)
        if (!manifestFile.exists()) manifestFile.redownloadVersionManifest()
        return try {
            manifestFile.findVersionJsonUrl(version)
        } catch (ex: JsonParseException) {
            null
        } ?: manifestFile.let {
            // Manifest file is corrupted or not up-to-date - redownload and try again.
            it.redownloadVersionManifest()
            it.findVersionJsonUrl(version)
        }
        ?: error("Invalid version $version")
    }

    private fun File.findVersionJsonUrl(version: String) =
        fromJson<JsonObject>()["versions"].asJsonArray.map { it.asJsonObject }
            .find { it["id"].asString == version }
            ?.let { it["url"].asString }

    private fun File.redownloadVersionManifest() = download(VERSION_MANIFEST_URL, this, ignoreInitialState = true)

    private val TargetData.versionJsonPath get() = "versions/$version/manifest.json"
    private val TargetData.sourceSetName get() = "mc$version"
    private val TargetData.mcConfigName get() = "mc$version"
    private val TargetData.mcLibsConfigName get() = "mcLibs$version"
    private val TargetData.mcNativeLibsConfigName get() = "mcNativeLibs$version"
    private val TargetData.downloadClientName get() = "downloadClient$version"
    private val TargetData.downloadServerName get() = "downloadServer$version"
}