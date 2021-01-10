package me.dreamhopping.pml.gradle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.dreamhopping.pml.gradle.data.Extension
import me.dreamhopping.pml.gradle.data.TargetExt
import me.dreamhopping.pml.gradle.mc.McOs
import me.dreamhopping.pml.gradle.mc.MinecraftSetup
import me.dreamhopping.pml.gradle.mc.MinecraftSetup.cacheDir
import me.dreamhopping.pml.gradle.mc.MinecraftSetup.repoDir
import me.dreamhopping.pml.gradle.mc.data.manifest.VersionManifest
import me.dreamhopping.pml.gradle.tasks.TaskCreateModsJson
import me.dreamhopping.pml.gradle.tasks.TaskDownloadAssets
import me.dreamhopping.pml.gradle.tasks.TaskGenRunConfig
import me.dreamhopping.pml.gradle.tasks.TaskRunGame
import me.dreamhopping.pml.gradle.utils.Json
import me.dreamhopping.pml.gradle.utils.VersionUtil
import me.dreamhopping.pml.gradle.utils.http.Downloader
import me.dreamhopping.pml.gradle.utils.http.HttpRequest
import me.dreamhopping.pml.gradle.utils.version.MinecraftVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.ide.idea.IdeaPlugin
import java.io.File
import java.io.IOException

class PufferfishGradle : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply(JavaPlugin::class.java)
        target.plugins.apply(IdeaPlugin::class.java)
        val ext = Extension(target)
        target.extensions.add("minecraft", ext)

        target.afterEvaluate { proj ->
            proj.repositories.maven {
                it.setUrl(proj.gradle.repoDir.toURI().toURL())
                it.metadataSources { sources ->
                    sources.artifact()
                    sources.mavenPom()
                }
            }
            proj.repositories.maven {
                it.setUrl("https://libraries.minecraft.net")
                it.metadataSources { sources ->
                    sources.artifact()
                    sources.mavenPom()
                }
            }

            runBlocking {
                val manifest = withContext(Dispatchers.IO) {
                    proj.gradle.cacheDir.mkdirs()
                    val manifestFile = File(proj.gradle.cacheDir, "data/versions/manifest.json")

                    try {
                        val response = HttpRequest.get(
                            "https://launchermeta.mojang.com/mc/game/version_manifest.json",
                            "User-Agent" to "PufferfishGradle/1.0"
                        )

                        response.data.use {
                            if (response.successful) {
                                manifestFile.parentFile.mkdirs()
                                manifestFile.outputStream().use { output ->
                                    it.copyTo(output)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        if (!manifestFile.exists()) error("No manifest file found")
                    }

                    Json.parse<VersionManifest>(manifestFile.readText())
                }

                ext.targets.forEach {
                    val json = runBlocking { MinecraftSetup.getVersionJson(proj, it, manifest) }

                    val set = proj.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName(
                        TargetExt.getMcSourceSetName(it.version)
                    )

                    val version = MinecraftVersion.parse(it.version)

                    val nativeDir = File(proj.gradle.cacheDir, "natives/${it.version}")

                    target.tasks.register("copyNatives${it.version}", Copy::class.java) { task ->
                        proj.configurations.maybeCreate(TargetExt.getMcLibNativesConfigName(it.version))
                            .forEach { file ->
                                task.from(proj.zipTree(file)) { spec ->
                                    spec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                                    spec.includeEmptyDirs = false
                                }
                            }
                        task.include(File(System.mapLibraryName("*")).name)
                        task.into(nativeDir)
                    }

                    target.tasks.register("genRunConfigs${it.version}") { task ->
                        task.dependsOn("genServerRunConfig${it.version}", "genClientRunConfig${it.version}")
                    }

                    target.tasks.register("genClientRunConfig${it.version}", TaskGenRunConfig::class.java) { task ->
                        task.dependsOn("downloadAssets${it.version}", "copyNatives${it.version}")
                        task.runDir = it.runDir.absolutePath
                        task.arguments = arrayListOf()
                        if (McOs.current() == McOs.MAC && VersionUtil.usesLwjgl3(version)) {
                            task.vmArguments =
                                arrayListOf("-Djava.library.path=${nativeDir.absolutePath}", "-XstartOnFirstThread")
                        } else {
                            task.vmArguments = arrayListOf("-Djava.library.path=${nativeDir.absolutePath}")
                        }
                        task.assetDirectory = File(proj.gradle.cacheDir, "assets").absolutePath
                        task.assetIndex = json.assets
                        task.sourceSetName = set.name
                        task.mainClass = it.clientMainClass
                        task.configName = "Minecraft ${it.version} Client"
                        task.select = true
                        task.dependsOn(if (ext.separateVersionJars) ext.mainSourceSet.jarTaskName else set.jarTaskName)
                    }

                    target.tasks.register("genServerRunConfig${it.version}", TaskGenRunConfig::class.java) { task ->
                        task.runDir = it.runDir.absolutePath
                        task.arguments = arrayListOf()
                        task.vmArguments = arrayListOf()
                        task.assetDirectory = File(proj.gradle.cacheDir, "assets").absolutePath
                        task.assetIndex = json.assets
                        task.sourceSetName = set.name
                        task.mainClass = it.serverMainClass
                        task.configName = "Minecraft ${it.version} Server"
                        task.server = true
                    }

                    target.tasks.register("run${it.version}Client", TaskRunGame::class.java) { task ->
                        task.dependsOn("downloadAssets${it.version}", "copyNatives${it.version}")
                        task.runDir = it.runDir.absolutePath
                        task.arguments = arrayListOf()
                        if (McOs.current() == McOs.MAC && VersionUtil.usesLwjgl3(version)) {
                            task.vmArguments =
                                arrayListOf("-Djava.library.path=${nativeDir.absolutePath}", "-XstartOnFirstThread")
                        } else {
                            task.vmArguments = arrayListOf("-Djava.library.path=${nativeDir.absolutePath}")
                        }
                        task.assetDirectory = File(proj.gradle.cacheDir, "assets").absolutePath
                        task.assetIndex = json.assets
                        task.classpath = set.runtimeClasspath
                        task.mainClass = it.clientMainClass
                        task.group = "minecraft"
                        task.dependsOn(if (ext.separateVersionJars) ext.mainSourceSet.jarTaskName else set.jarTaskName)
                    }

                    target.tasks.register("run${it.version}Server", TaskRunGame::class.java) { task ->
                        task.runDir = it.runDir.absolutePath
                        task.arguments = arrayListOf()
                        task.vmArguments = arrayListOf()
                        task.assetDirectory = File(proj.gradle.cacheDir, "assets").absolutePath
                        task.assetIndex = json.assets
                        task.group = "minecraft"
                        task.classpath = set.runtimeClasspath
                        task.mainClass = it.serverMainClass
                        task.server = true
                    }

                    target.tasks.register("downloadAssets${it.version}", TaskDownloadAssets::class.java) { task ->
                        json.assetIndex ?: error("Version json does not have asset index")
                        val outputDir = File(proj.gradle.cacheDir, "assets")
                        task.output = outputDir.absolutePath
                        val assetIndexFile = File(proj.gradle.cacheDir, "assets/indexes/${json.assets}.json")
                        runBlocking {
                            Downloader.download(
                                json.assetIndex.url ?: error("Version json does not have asset index"),
                                assetIndexFile,
                                json.assetIndex.sha1
                            )
                        }
                        task.index = Json.parse(assetIndexFile.readText())
                    }

                    if (ext.separateVersionJars) {
                        val task = proj.tasks.getByName(set.jarTaskName)
                        task.dependsOn(ext.mainSourceSet.classesTaskName)
                        (task as Jar).from(ext.mainSourceSet.output)
                        proj.tasks.getByName("assemble").dependsOn(task.name)
                    } else {
                        val task = proj.tasks.getByName(ext.mainSourceSet.jarTaskName) as Jar
                        task.dependsOn(set.classesTaskName)
                        task.from(set.output)
                    }

                    val config = proj.configurations.maybeCreate("pgMappings${it.version}")
                    it.mappings.addDataToConfiguration(proj, config, it.mappingVersion)
                    MinecraftSetup.setupVersion(proj, it, ext.mainSourceSet, config, manifest)
                }
            }
            target.tasks.register("genRunConfigs") { task ->
                task.group = "minecraft"
                task.dependsOn(*ext.targets.map { "genRunConfigs${it.version}" }.toTypedArray())
            }

            val res = (target.tasks.getByName(ext.mainSourceSet.processResourcesTaskName) as AbstractCopyTask)
            res.dependsOn("genModsJson")
            val task = target.tasks.register("genModsJson", TaskCreateModsJson::class.java) { task ->
                task.mods = ext.modContainer.toList()
                task.output = File(task.temporaryDir, "mods.json")
            }
            res.from(task.get().output)
        }
    }
}