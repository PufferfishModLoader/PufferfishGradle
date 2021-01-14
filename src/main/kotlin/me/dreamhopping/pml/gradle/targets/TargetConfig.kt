package me.dreamhopping.pml.gradle.targets

import me.dreamhopping.pml.gradle.PGExtension
import me.dreamhopping.pml.gradle.data.version.VersionJson
import me.dreamhopping.pml.gradle.tasks.decompile.DecompileTask
import me.dreamhopping.pml.gradle.tasks.download.*
import me.dreamhopping.pml.gradle.tasks.map.MapJarTask
import me.dreamhopping.pml.gradle.tasks.map.gen.GenMappingsTask
import me.dreamhopping.pml.gradle.tasks.merge.MergeJarsTask
import me.dreamhopping.pml.gradle.tasks.run.ExtractNativesTask
import me.dreamhopping.pml.gradle.tasks.run.GenRunConfigsTask
import me.dreamhopping.pml.gradle.tasks.run.RunTask
import me.dreamhopping.pml.gradle.tasks.strip.StripJarTask
import me.dreamhopping.pml.gradle.util.*
import me.dreamhopping.pml.gradle.util.Json.fromJson
import me.dreamhopping.pml.runtime.start.Start
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.jvm.tasks.Jar
import java.net.URL

object TargetConfig {
    fun onIdChange(project: Project, ext: TargetExtension, version: String) {
        val config = project.configurations.maybeCreate("mc$version")
        config.dependencies.removeAll { true }
        project.dependencies.add(
            config.name,
            "net.minecraft:merged:${MapJarTask.getVersion(version, ext.mappingProviders, ext.accessTransformers)}"
        )
        project.dependencies.add(config.name, "net.minecraft:merged-resources:$version")
    }

    fun onSourceSetNameChange(ext: TargetExtension, old: String) {
        try {
            val oldSet = ext.project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName(old)
            ext.project.rm(oldSet.jarTaskName)
            ext.project.rm(oldSet.javadocJarTaskName)
            ext.project.rm(oldSet.javadocTaskName)
            ext.project.rm(oldSet.sourcesJarTaskName)
            ext.project.rm(oldSet.compileJavaTaskName)
            ext.project.rm(oldSet.processResourcesTaskName)
            ext.project.rm(oldSet.classesTaskName)
            ext.project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.remove(oldSet)
        } catch (ignored: UnknownDomainObjectException) {
        }
        if (ext.minecraft.separateVersionJars) setUpJarTask(ext)
    }

    private fun Project.rm(name: String) {
        try {
            val task = tasks.getByName(name)
            task.enabled = false
            task.group = "other"
        } catch (ignored: UnknownDomainObjectException) {
        }
    }

    fun setUpJarTask(ext: TargetExtension) {
        val project = ext.project
        val convention = project.convention
        val plugin = convention.getPlugin(JavaPluginConvention::class.java)
        val set = plugin.sourceSets.maybeCreate(ext.sourceSetName)
        val taskName = set.jarTaskName
        project.tasks.register(taskName, Jar::class.java) {
            it.from(set.output)
            it.dependsOn(set.classesTaskName, set.processResourcesTaskName)
            it.archiveClassifier.set(ext.sourceSetName)
            it.group = "build"
        }
    }

    fun setupTarget(ext: TargetExtension) {
        val project = ext.project

        val libsConfig = project.configurations.maybeCreate("mcLibs${ext.version}")
        val nativeLibsConfig = project.configurations.maybeCreate("mcNatives${ext.version}")
        val mcConfig = project.configurations.maybeCreate("mc${ext.version}")
        mcConfig.isCanBeResolved = false
        libsConfig.extendsFrom(nativeLibsConfig)

        project.tasks.register("$DOWNLOAD_VERSION_BASE_NAME${ext.version}", DownloadVersionJsonTask::class.java) {
            it.dependsOn(DOWNLOAD_MANIFEST_NAME)
            it.manifest = it.project.get<DownloadTask>(DOWNLOAD_MANIFEST_NAME).output
            it.version = ext.version
            it.output = it.project.getCachedFile("versions/${it.version}.json")
        }

        project.tasks.register("$DOWNLOAD_CLIENT_BASE_NAME${ext.version}", DownloadFromVersionJsonTask::class.java) {
            it.dependsOn("$DOWNLOAD_VERSION_BASE_NAME${ext.version}")
            it.versionJson = it.project.get<DownloadTask>("$DOWNLOAD_VERSION_BASE_NAME${ext.version}").output
            it.getUrlFromJson = { downloads["client"]?.url ?: error("No client download URL") }
            it.output = it.project.getRepoFile("net.minecraft", "client", ext.version)
        }

        project.tasks.register("$DOWNLOAD_SERVER_BASE_NAME${ext.version}", DownloadFromVersionJsonTask::class.java) {
            it.dependsOn("$DOWNLOAD_VERSION_BASE_NAME${ext.version}")
            it.versionJson = it.project.get<DownloadTask>("$DOWNLOAD_VERSION_BASE_NAME${ext.version}").output
            it.getUrlFromJson = { downloads["server"]?.url ?: error("No server download URL") }
            it.output = it.project.getRepoFile("net.minecraft", "server", ext.version)
        }

        project.tasks.register("$STRIP_CLIENT_BASE_NAME${ext.version}", StripJarTask::class.java) {
            it.dependsOn("$DOWNLOAD_CLIENT_BASE_NAME${ext.version}")
            it.input = it.project.get<DownloadTask>("$DOWNLOAD_CLIENT_BASE_NAME${ext.version}").output
            it.allowedDirectories += "net/minecraft"
            it.allowedDirectories += "com/mojang"
            it.classOutput = it.project.getRepoFile("net.minecraft", "client", ext.version, "classes")
            it.resourceOutput = it.project.getRepoFile("net.minecraft", "client", ext.version, "resources")
        }

        project.tasks.register("$STRIP_SERVER_BASE_NAME${ext.version}", StripJarTask::class.java) {
            it.dependsOn("$DOWNLOAD_SERVER_BASE_NAME${ext.version}")
            it.input = it.project.get<DownloadTask>("$DOWNLOAD_SERVER_BASE_NAME${ext.version}").output
            it.allowedDirectories += "net/minecraft"
            it.classOutput = it.project.getRepoFile("net.minecraft", "server", ext.version, "classes")
            it.resourceOutput = it.project.getRepoFile("net.minecraft", "server", ext.version, "resources")
        }

        project.tasks.register("$MERGE_CLASSES_BASE_NAME${ext.version}", MergeJarsTask::class.java) {
            it.dependsOn("$STRIP_CLIENT_BASE_NAME${ext.version}", "$STRIP_SERVER_BASE_NAME${ext.version}")
            val stripClient: StripJarTask = it.project["$STRIP_CLIENT_BASE_NAME${ext.version}"]
            val stripServer: StripJarTask = it.project["$STRIP_SERVER_BASE_NAME${ext.version}"]
            it.clientJar = stripClient.classOutput
            it.serverJar = stripServer.classOutput
            it.outputJar = it.project.getRepoFile("net.minecraft", "merged", ext.version)
        }

        project.tasks.register("$MERGE_RESOURCES_BASE_NAME${ext.version}", MergeJarsTask::class.java) {
            it.dependsOn("$STRIP_CLIENT_BASE_NAME${ext.version}", "$STRIP_SERVER_BASE_NAME${ext.version}")
            val stripClient: StripJarTask = it.project["$STRIP_CLIENT_BASE_NAME${ext.version}"]
            val stripServer: StripJarTask = it.project["$STRIP_SERVER_BASE_NAME${ext.version}"]
            it.clientJar = stripClient.resourceOutput
            it.serverJar = stripServer.resourceOutput
            it.outputJar = it.project.getRepoFile("net.minecraft", "merged-resources", ext.version)
        }

        val genMapTask =
            project.tasks.register("$GENERATE_MAPPINGS_BASE_NAME${ext.version}", GenMappingsTask::class.java) {
                it.providers = ext.mappingProviders
                it.outputBase = project.getCachedFile("mappings/merged-${ext.version}").absolutePath
            }

        val mapJarTask = project.tasks.register("$MAP_BASE_NAME${ext.version}", MapJarTask::class.java) {
            it.dependsOn("$MERGE_CLASSES_BASE_NAME${ext.version}", "$GENERATE_MAPPINGS_BASE_NAME${ext.version}")
            it.providers = ext.mappingProviders
            it.input = it.project.get<MergeJarsTask>("$MERGE_CLASSES_BASE_NAME${ext.version}").outputJar
            it.version = ext.version
            it.genTask = genMapTask
            it.accessTransformers = ext.accessTransformers
        }

        val genSourcesTask = project.tasks.register("$GEN_SOURCES_BASE_NAME${ext.version}", DecompileTask::class.java) {
            it.dependsOn(
                "$MAP_BASE_NAME${ext.version}",
                "$GENERATE_MAPPINGS_BASE_NAME${ext.version}",
                DOWNLOAD_FERNFLOWER_NAME
            )
            it.providers = ext.mappingProviders
            it.version = ext.version
            it.genTask = genMapTask
            it.mapJarTask = mapJarTask
            it.fernflower = project.get<DownloadTask>(DOWNLOAD_FERNFLOWER_NAME).output
            it.libraries = libsConfig
        }

        val assetIndexDownloadName = "$DOWNLOAD_ASSET_INDEX_BASE_NAME${ext.version}"
        project.tasks.register(assetIndexDownloadName, DownloadFromVersionJsonWithVariableOutputTask::class.java) {
            it.dependsOn("$DOWNLOAD_VERSION_BASE_NAME${ext.version}")
            it.versionJson = project.get<DownloadTask>("$DOWNLOAD_VERSION_BASE_NAME${ext.version}").output
            it.getUrlFromJson = { assetIndex.url!! }
            it.getOutput = { project.getCachedFile("assets/indexes/$assets.json") }
        }

        project.tasks.register("$DOWNLOAD_ASSETS_BASE_NAME${ext.version}", DownloadAssetsTask::class.java) {
            it.dependsOn(assetIndexDownloadName)
            it.versionJson = project.get<DownloadTask>("$DOWNLOAD_VERSION_BASE_NAME${ext.version}").output
            it.outputDirectory = project.getCachedFile("assets/objects")
        }

        val extractNativesTask =
            project.tasks.register("extractNatives${ext.version}", ExtractNativesTask::class.java) { task ->
                task.config = nativeLibsConfig
                task.outputDir = project.getCachedFile("natives/${ext.version}")
                task.pattern =
                    "^${System.mapLibraryName("<WILDCARD>+").replace(".", "\\.").replace("<WILDCARD>", ".")}\$"
            }

        val genRunConfigClientTask =
            project.tasks.register("genClientRunConfig${ext.version}", GenRunConfigsTask::class.java) {
                it.dependsOn("$DOWNLOAD_ASSETS_BASE_NAME${ext.version}", "extractNatives${ext.version}")
                val task: DownloadTask = project["$DOWNLOAD_VERSION_BASE_NAME${ext.version}"]
                it.dependsOn(task.name)
                it.vmArgs = if (Os.isFamily(Os.FAMILY_MAC)) listOf(
                    "-XstartOnFirstThread",
                    "-Djava.library.path=${project.getCachedFile("natives/${ext.version}")}"
                ) else listOf("-Djava.library.path=${project.getCachedFile("natives/${ext.version}")}")
                it.args = emptyList()
                it.environment = mapOf(
                    "PG_IS_SERVER" to { "false" },
                    "PG_ASSET_INDEX" to { task.output.fromJson<VersionJson>().assets },
                    "PG_ASSETS_DIR" to { project.getCachedFile("assets").absolutePath },
                    "PG_MAIN_CLASS" to { ext.clientMainClass }
                )
                it.mainClass = Start::class.java.name
                it.configName = "Minecraft ${ext.version} Client"
                it.select = true
            }

        val genRunConfigServerTask =
            project.tasks.register("genServerRunConfig${ext.version}", GenRunConfigsTask::class.java) {
                it.vmArgs = emptyList()
                it.args = emptyList()
                it.environment = mapOf(
                    "PG_IS_SERVER" to { "true" },
                    "PG_MAIN_CLASS" to { ext.serverMainClass }
                )
                it.mainClass = Start::class.java.name
                it.configName = "Minecraft ${ext.version} Server"
            }

        project.tasks.register("genRunConfigs${ext.version}") {
            it.dependsOn(genRunConfigClientTask.name, genRunConfigServerTask.name)
        }

        val runClientTask = project.tasks.register("runClient${ext.version}", RunTask::class.java) {
            it.dependsOn("$DOWNLOAD_ASSETS_BASE_NAME${ext.version}", "extractNatives${ext.version}")
            val task: DownloadTask = project["$DOWNLOAD_VERSION_BASE_NAME${ext.version}"]
            it.dependsOn(task.name)
            it.vmArgs = if (Os.isFamily(Os.FAMILY_MAC)) listOf(
                "-XstartOnFirstThread",
                "-Djava.library.path=${project.getCachedFile("natives/${ext.version}")}"
            ) else listOf("-Djava.library.path=${project.getCachedFile("natives/${ext.version}")}")
            it.group = "minecraft"
            it.args = emptyList()
            it.environment = mapOf(
                "PG_IS_SERVER" to { "false" },
                "PG_ASSET_INDEX" to { task.output.fromJson<VersionJson>().assets },
                "PG_ASSETS_DIR" to { project.getCachedFile("assets").absolutePath },
                "PG_MAIN_CLASS" to { ext.clientMainClass }
            )
            it.mainClass = Start::class.java.name
        }

        val runServerTask = project.tasks.register("runServer${ext.version}", RunTask::class.java) {
            it.vmArgs = emptyList()
            it.group = "minecraft"
            it.args = emptyList()
            it.environment = mapOf(
                "PG_IS_SERVER" to { "true" },
                "PG_MAIN_CLASS" to { ext.serverMainClass }
            )
            it.mainClass = Start::class.java.name
        }

        project.afterEvaluate { _ ->
            val versionJson = project.getCachedFile("versions/${ext.version}.json")

            if (versionJson.exists()) {
                val json = versionJson.fromJson<VersionJson>()
                json.libraries.forEach {
                    if (it.isAllowed()) {
                        if (it.natives == null) {
                            project.dependencies.add(libsConfig.name, it.name)
                        } else {
                            project.dependencies.add(nativeLibsConfig.name, "${it.name}:${it.getNative()}")
                        }
                    }
                }
            } else {
                project.tasks.register("addLibraries${ext.version}", AddLibrariesTask::class.java) {
                    it.dependsOn("$DOWNLOAD_VERSION_BASE_NAME${ext.version}")
                    it.versionJson = project.get<DownloadTask>("$DOWNLOAD_VERSION_BASE_NAME${ext.version}").output
                    it.configuration = libsConfig
                    it.nativesConfiguration = nativeLibsConfig
                }
                genSourcesTask.configure {
                    it.dependsOn("addLibraries${ext.version}")
                }
                extractNativesTask.configure {
                    it.dependsOn("addLibraries${ext.version}")
                }
            }

            project.dependencies.add(libsConfig.name, "me.dreamhopping.pml:gradle:3.0.0:runtime")

            val inputs = ext.mappingProviders.mapIndexed { index, it ->
                val id = "${ext.version}-$index"
                it.fetchIdFromDiskIfPossible(project, ext.mappingVersion)
                it.setUpVersionSetupTasks(project, id) { ext.mappingVersion }
            }

            genMapTask.configure { task ->
                task.dependsOn(*inputs.map { it.first }.toTypedArray())
                task.inputProviders = inputs.map { it.second }
            }

            val set =
                project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.maybeCreate(ext.sourceSetName)
            val mainSourceSet = ext.minecraft.mainSourceSet

            if (!ext.minecraft.separateVersionJars) {
                val jarTask: Jar = project[mainSourceSet.jarTaskName]
                jarTask.from(set.output)
                jarTask.dependsOn(set.processResourcesTaskName, set.compileJavaTaskName)
            } else {
                project.tasks.getByName("assemble").dependsOn(set.jarTaskName)
            }

            genRunConfigClientTask.configure {
                it.sourceSetNameGetter = { set.name }
                it.workDir = ext.runDir.absolutePath
            }

            genRunConfigServerTask.configure {
                it.sourceSetNameGetter = { set.name }
                it.workDir = ext.runDir.absolutePath
            }

            runClientTask.configure {
                it.dependsOn(set.classesTaskName)
                it.workDir = ext.runDir.absolutePath
                it.classpath = set.runtimeClasspath
            }

            runServerTask.configure {
                it.dependsOn(set.classesTaskName)
                it.workDir = ext.runDir.absolutePath
                it.classpath = set.runtimeClasspath
            }

            project.get<Task>(set.compileJavaTaskName).dependsOn(
                mapJarTask.name,
                "$MERGE_RESOURCES_BASE_NAME${ext.version}"
            )

            mcConfig.extendsFrom(libsConfig)
            project.configurations.getByName(set.implementationConfigurationName).extendsFrom(mcConfig)
        }

        project.tasks.register("setup${ext.version}") {
            it.dependsOn("$MAP_BASE_NAME${ext.version}", "$MERGE_RESOURCES_BASE_NAME${ext.version}")
        }
    }

    fun setup(ext: PGExtension) {
        val project = ext.project

        project.repositories.maven {
            it.setUrl(project.repoDir.toURI().toURL())
            it.metadataSources.artifact()
        }

        project.tasks.register(DOWNLOAD_MANIFEST_NAME, DownloadTask::class.java) {
            it.url = URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")
            it.downloadEvenIfNotNecessary = true
            it.output = it.project.getCachedFile("manifest.json")
        }

        project.tasks.register(DOWNLOAD_FERNFLOWER_NAME, DownloadTask::class.java) {
            it.url =
                URL("https://maven.fabricmc.net/net/fabricmc/fabric-fernflower/1.3.0/fabric-fernflower-1.3.0.jar")
            it.output = it.project.getRepoFile("net.fabricmc", "fabric-fernflower", "1.3.0")
        }

        val setupTask = project.tasks.register("setup") {
            it.group = "minecraft"
        }
        val genRunConfigsTask = project.tasks.register("genRunConfigs") {
            it.group = "minecraft"
        }
        project.afterEvaluate { _ ->
            setupTask.configure { task ->
                task.dependsOn(*ext.targets.map { "setup${it.version}" }.toTypedArray())
            }
            genRunConfigsTask.configure { task ->
                task.dependsOn(*ext.targets.map { "genRunConfigs${it.version}" }.toTypedArray())
            }
        }
    }
}