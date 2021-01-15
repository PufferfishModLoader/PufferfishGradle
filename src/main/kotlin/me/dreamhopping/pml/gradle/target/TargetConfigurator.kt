package me.dreamhopping.pml.gradle.target

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import me.dreamhopping.pml.gradle.data.minecraft.VersionJson
import me.dreamhopping.pml.gradle.user.TargetData
import me.dreamhopping.pml.gradle.user.UserData
import me.dreamhopping.pml.gradle.util.dataFile
import me.dreamhopping.pml.gradle.util.download
import me.dreamhopping.pml.gradle.util.fromJson
import me.dreamhopping.pml.gradle.util.java
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
        println(version)
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
}