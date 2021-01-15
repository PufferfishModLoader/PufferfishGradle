package me.dreamhopping.pml.gradle.target

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import me.dreamhopping.pml.gradle.data.minecraft.VersionJson
import me.dreamhopping.pml.gradle.target.ArtifactVersionGenerator.buildMappedJarArtifactVersion
import me.dreamhopping.pml.gradle.tasks.download.DownloadTask
import me.dreamhopping.pml.gradle.tasks.map.apply.ApplyMappingsTask
import me.dreamhopping.pml.gradle.tasks.map.generate.GenerateMappingsTask
import me.dreamhopping.pml.gradle.tasks.merge.MergeTask
import me.dreamhopping.pml.gradle.tasks.strip.StripTask
import me.dreamhopping.pml.gradle.user.TargetData
import me.dreamhopping.pml.gradle.user.UserData
import me.dreamhopping.pml.gradle.util.*
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import java.io.File

object TargetConfigurator {
    private const val VERSION_MANIFEST_PATH = "versions/manifest.json"
    private const val VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
    private val readOnlyProjects = hashSetOf<Project>()

    fun configureTarget(project: Project, target: TargetData, parent: UserData, addDefaultMaps: Boolean) {
        if (parent.separateVersionJars) setUpJarTasks(project, target)

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

        if (version.downloads.server == null) {
            project.dependencies.add(minecraftConfiguration.name, "net.minecraft:client:${target.version}:resources")
        } else {
            project.dependencies.add(minecraftConfiguration.name, "net.minecraft:resources:${target.version}")
        }
        refreshMcDep(project, target)

        val downloadClientTask = project.tasks.register(target.downloadClientName, DownloadTask::class.java) {
            it.url = version.downloads.client.url
            it.sha1 = version.downloads.client.sha1
            it.output = project.repoFile("net.minecraft", "client", target.version)
        }

        val downloadServerTask = version.downloads.server?.let {
            project.tasks.register(target.downloadServerName, DownloadTask::class.java) { task ->
                task.url = it.url
                task.sha1 = it.sha1
                task.output = project.repoFile("net.minecraft", "server", target.version)
            }
        }

        val stripClientTask =
            StripTask.register(target.stripClientName, "client", target.version, project, downloadClientTask)
        val stripServerTask = downloadServerTask?.let {
            StripTask.register(target.stripServerName, "server", target.version, project, it)
        }
        val mergeClassesTask = stripServerTask?.let {
            project.tasks.register(target.mergeClassesName, MergeTask::class.java) {
                it.dependsOn(stripClientTask.name, stripServerTask.name)
                it.clientJar = stripClientTask.get().classOutput
                it.serverJar = stripServerTask.get().classOutput
                it.outputJar = project.repoFile("net.minecraft", "merged", target.version)
            }
        }
        val mergeResourcesTask = stripServerTask?.let {
            project.tasks.register(target.mergeResourcesName, MergeTask::class.java) {
                it.dependsOn(stripClientTask.name, stripServerTask.name)
                it.clientJar = stripClientTask.get().resourceOutput
                it.serverJar = stripServerTask.get().resourceOutput
                it.outputJar = project.repoFile("net.minecraft", "resources", target.version)
            }
        }

        val generateMappingsTask = project.tasks.register(target.generateMappingsName, GenerateMappingsTask::class.java) {
            it.mappingProviders = target.mappings
            it.outputFile = project.repoFile("net.minecraft", "mapped", target.buildMappedJarArtifactVersion(), "mappings", "json")
        }

        val deobfuscateTask = project.tasks.register(target.deobfuscateName, ApplyMappingsTask::class.java) {
            it.dependsOn(generateMappingsTask.name, mergeClassesTask?.name ?: stripClientTask.name)
            it.inputJar = mergeClassesTask?.get()?.outputJar ?: stripClientTask.get().classOutput
            it.mappings = generateMappingsTask.get().outputFile
            it.accessTransformers = target.accessTransformers
            it.outputJar = project.repoFile("net.minecraft", "mapped", target.buildMappedJarArtifactVersion())
        }

        project.tasks.register(target.setupName) {
            it.dependsOn(deobfuscateTask.name)
            mergeResourcesTask?.apply { it.dependsOn(name) }
        }

        project.afterEvaluate {
            // We could technically do this outside of afterEvaluate, but we change the dependencies of these
            // configurations quite a bit before this point, and doing it here ensures it does not get resolved accidentally.
            readOnlyProjects.add(project) // From this point on, changes to TargetData will not do anything.
            val sourceSet = it.java.sourceSets.maybeCreate(target.sourceSetName)

            project.configurations.getByName(sourceSet.implementationConfigurationName)
                .extendsFrom(minecraftConfiguration)
        }
    }

    fun setUpGlobalTasks(project: Project, data: UserData) {
        val setupTask = project.tasks.register("setup") {
            it.group = "minecraft"
        }
        project.afterEvaluate {
            setupTask.configure { task ->
                task.dependsOn(*data.targets.map { it.setupName }.toTypedArray())
            }
        }
    }

    fun setUpJarTasks(project: Project, target: TargetData) {
        val set = project.java.sourceSets.maybeCreate(target.sourceSetName)
        project.tasks.register(set.jarTaskName, Jar::class.java) {
            it.dependsOn(set.compileJavaTaskName, set.processResourcesTaskName)
            it.from(set.output)
            it.archiveClassifier.set(set.name)
            it.group = "build"
        }
    }

    fun refreshMcDep(project: Project, target: TargetData) {
        if (project in readOnlyProjects) return
        val config = project.configurations.maybeCreate(target.mcConfigName)
        config.dependencies.removeIf {
            it.group == "net.minecraft" && it.name == "mapped"
        }
        val version = target.buildMappedJarArtifactVersion()
        project.dependencies.add(config.name, "net.minecraft:mapped:$version")
        project.tasks.findByName(target.generateMappingsName)?.let {
            it as GenerateMappingsTask
            it.outputFile = project.repoFile("net.minecraft", "mapped", version, "mappings", "json")
        }
        project.tasks.findByName(target.deobfuscateName)?.let {
            it as ApplyMappingsTask
            it.outputJar = project.repoFile("net.minecraft", "mapped", version)
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
    private val TargetData.stripClientName get() = "stripClient$version"
    private val TargetData.stripServerName get() = "stripServer$version"
    private val TargetData.mergeClassesName get() = "mergeClasses$version"
    private val TargetData.mergeResourcesName get() = "mergeResources$version"
    private val TargetData.generateMappingsName get() = "generateMappings$version"
    private val TargetData.deobfuscateName get() = "deobfuscate$version"
    private val TargetData.setupName get() = "setup$version"
}