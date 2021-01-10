package me.dreamhopping.pml.gradle.mappings

import com.google.gson.Gson
import com.google.gson.JsonObject
import me.dreamhopping.pml.gradle.mappings.data.McpVersion
import me.dreamhopping.pml.gradle.mc.MinecraftSetup.cacheDir
import me.dreamhopping.pml.gradle.utils.Json
import me.dreamhopping.pml.gradle.utils.http.HttpRequest
import me.dreamhopping.pml.gradle.utils.version.MinecraftVersion
import me.dreamhopping.pml.gradle.utils.version.ReleaseVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class McpMappingProvider : MappingProvider() {
    private var channel: String? = null
    private var version: String? = null

    fun version(channel: String, version: String) {
        this.channel = channel
        this.version = version
    }

    override suspend fun addDataToConfiguration(project: Project, configuration: Configuration, requestedVersion: String) {
        val srgConfig = project.configurations.maybeCreate(createIntermediaryConfigName(requestedVersion))

        val (channel, mappingVersion) = getMappingVersion(project, requestedVersion)

        project.repositories
            .maven { it.setUrl("https://files.minecraftforge.net/maven") }
            .metadataSources {
                it.artifact()
                it.mavenPom()
            }
        project.dependencies.add(configuration.name, mapOf(
            "group" to "de.oceanlabs.mcp",
            "name" to "mcp_$channel",
            "version" to "$mappingVersion-$requestedVersion",
            "ext" to "zip"
        ))

        val version = MinecraftVersion.parse(requestedVersion)
        if (version !is ReleaseVersion) error("PufferfishGradle does not support MCP for non-release versions")
        if (version.minor < 13) {
            project.dependencies.add(srgConfig.name, mapOf(
                "group" to "de.oceanlabs.mcp",
                "name"  to "mcp",
                "version" to requestedVersion,
                "classifier" to "srg",
                "ext" to "zip"
            ))
        } else {
            project.dependencies.add(srgConfig.name, mapOf(
                "group" to "de.oceanlabs.mcp",
                "name" to "mcp_config",
                "version" to requestedVersion,
                "ext" to "zip"
            ))
        }
    }

    override suspend fun createId(project: Project, requestedVersion: String): String {
        val (channel, version) = getMappingVersion(project, requestedVersion)
        return "mcp-$channel-$version"
    }

    override fun loadIgnoringCache(project: Project, config: Configuration, requestedVersion: String): MappingInfo {
        val srgConfig = project.configurations.maybeCreate(createIntermediaryConfigName(requestedVersion))
        val version = MinecraftVersion.parse(requestedVersion)
        if (version !is ReleaseVersion) error("PufferfishGradle does not support MCP for non-release versions")

        val fieldsCsv = hashMapOf<String, String>()
        val methodsCsv = hashMapOf<String, String>()

        for (file in config) {
            if (file.extension == "zip") {
                ZipFile(file).use {
                    it.loadNamesFromZipEntry(it.getEntry("fields.csv"), fieldsCsv)
                    it.loadNamesFromZipEntry(it.getEntry("methods.csv"), methodsCsv)
                }
            }
        }

        val classes = linkedMapOf<String, String>()
        val methods = linkedMapOf<Pair<String, String>, String>()
        val fields = linkedMapOf<String, String>()
        val packages = linkedMapOf<String, String>()

        if (version.minor < 13) {
            for (file in srgConfig) {
                if (file.extension == "zip") {
                    ZipFile(file).use { zip ->
                        zip.getInputStream(zip.getEntry("joined.srg")).bufferedReader().use { stream ->
                            stream.lines().forEach {
                                val parts = it.split(" ")
                                when (parts[0]) {
                                    "CL:" -> classes[parts[1]] = parts[2]
                                    "FD:" -> fields[parts[1]] = mapName(parts[2], fieldsCsv)
                                    "MD:" -> methods[parts[1] to parts[2]] = mapName(parts[3], methodsCsv)
                                    "PK:" -> packages[parts[1]] = parts[2]
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (file in srgConfig) {
                if (file.extension == "zip") {
                    ZipFile(file).use { zip ->
                        zip.getInputStream(zip.getEntry("config/joined.tsrg")).bufferedReader().use { stream ->
                            var currentClass = ""
                            stream.lines().forEach {
                                if (it.startsWith('\t') || it.startsWith(" ")) {
                                    val parts = it.trim().split(" ")
                                    if (parts.size == 3) {
                                        // Method
                                        methods["$currentClass/${parts[0]}" to parts[1]] = methodsCsv[parts[2]] ?: parts[2]
                                    } else {
                                        fields["$currentClass/${parts[0]}"] = fieldsCsv[parts[1]] ?: parts[1]
                                    }
                                } else {
                                    val parts = it.split(" ")
                                    currentClass = parts[0]
                                    classes[parts[0]] = parts[1]
                                }
                            }
                        }
                    }
                }
            }
        }

        return MappingInfo(classes, fields, methods, packages)
    }

    private fun mapName(src: String, csv: Map<String, String>): String {
        val idx = src.lastIndexOf('/')
        val name = src.substring(idx + 1)
        return csv[name] ?: name
    }

    private fun ZipFile.loadNamesFromZipEntry(entry: ZipEntry, map: MutableMap<String, String>) {
        getInputStream(entry).bufferedReader().use { reader ->
            reader.lines().forEach {
                val parts = it.split(",")
                map[parts[0]] = parts[1]
            }
        }
    }

    private suspend fun getMappingVersion(project: Project, version: String): Pair<String, String> {
        if (this.channel != null && this.version != null) {
            return channel!! to this.version!!
        }

        val versionFile = File(project.gradle.cacheDir, "data/mcp/versions.json")

        try {
            val response = HttpRequest.get(
                "http://export.mcpbot.bspk.rs/versions.json",
                "User-Agent" to "PufferfishGradle/1.0"
            )

            response.data.use {
                if (response.successful) {
                    versionFile.parentFile.mkdirs()
                    versionFile.outputStream().use { output ->
                        it.copyTo(output)
                    }
                }
            }
        } catch (e: IOException) {
            if (!versionFile.exists()) error("No MCP version list found")
        }

        val versionObj = Json.parse<JsonObject>(versionFile.readText())
        val ver = versionObj.entrySet().find { it.key == version }?.value?.let { Gson().fromJson(it, McpVersion::class.java) } ?: error("No MCP mappings for version $version")

        return when {
            ver.stable.isNotEmpty() -> "stable" to ver.stable[0].toString()
            ver.snapshot.isNotEmpty() -> "snapshot" to ver.snapshot[0].toString()
            else -> error("No MCP mappings for version $version")
        }
    }

    private fun createIntermediaryConfigName(version: String) = "pgIntermediaryMappings$version"
}