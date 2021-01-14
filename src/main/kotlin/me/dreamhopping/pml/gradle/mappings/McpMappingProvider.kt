package me.dreamhopping.pml.gradle.mappings

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.dreamhopping.pml.gradle.tasks.download.DownloadTask
import me.dreamhopping.pml.gradle.tasks.map.mcp.DownloadMcpMappingsTask
import me.dreamhopping.pml.gradle.tasks.map.mcp.LoadMcpMappingsTask
import me.dreamhopping.pml.gradle.util.*
import me.dreamhopping.pml.gradle.util.Json.fromJson
import org.gradle.api.Project
import java.io.File
import java.net.URL

class McpMappingProvider(channel: String? = null, version: String? = null, private val onIdChange: () -> Unit) : MappingProvider {
    private var version = version
        set(v) {
            field = v
            onIdChange()
        }
    private var channel = channel
        set(v) {
            field = v
            onIdChange()
        }
    override val id get() = "mcp-$channel-$version"

    override fun fetchIdFromDiskIfPossible(project: Project, minecraftVersion: String) {
        if (channel == null || version == null) {
            val versionFile = project.getCachedFile("mcp/versions.json")

            if (versionFile.exists()) {
                consumeVersions(versionFile, minecraftVersion)
            }
        }
    }

    private fun consumeVersions(versionFile: File, minecraftVersion: String) {
        val versionObj = versionFile.fromJson<JsonObject>()
        val ver = versionObj.entrySet().find { it.key == minecraftVersion }?.value?.asJsonObject ?: error("No MCP mappings for version $minecraftVersion")

        val stable = ver["stable"].cast<JsonArray>()
        val snapshot = ver["snapshot"].cast<JsonArray>()

        val (channel, version) = when {
            stable.size() > 0 -> "stable" to stable[0].asString
            snapshot.size() > 0 -> "snapshot" to snapshot[0].asString
            else -> error("No MCP mappings for version $minecraftVersion")
        }

        this.channel = channel
        this.version = version
    }

    override fun setUpVersionSetupTasks(
        project: Project,
        id: String,
        minecraftVersion: () -> String
    ): Pair<String, () -> File> {
        manifestConsumers.add(this to minecraftVersion)
        val name = "${FETCH_MAPPING_VERSIONS_BASE_NAME}Mcp"
        val file = project.getCachedFile("mcp/versions.json")

        if ((version == null || channel == null) && project.tasks.findByName(name) == null) {
            project.tasks.register(name, DownloadTask::class.java) { task ->
                task.downloadEvenIfNotNecessary = true
                task.output = file
                task.url = URL("http://export.mcpbot.bspk.rs/versions.json")
                task.doLast {
                    manifestConsumers.forEach {
                        it.first.consumeVersions(file, it.second())
                    }
                }
            }
        }

        val downloadTask = project.tasks.register("$DOWNLOAD_MAPPINGS_BASE_NAME$id", DownloadMcpMappingsTask::class.java) {
            if (channel == null || version == null) it.dependsOn(name)
            it.getChannel = { channel ?: "stable" }
            it.getVersion = { "$version-${minecraftVersion()}" }
            it.getMinecraftVersion = minecraftVersion
        }

        val loadName = "$LOAD_MAPPINGS_BASE_NAME$id"
        val task = project.tasks.register("$LOAD_MAPPINGS_BASE_NAME$id", LoadMcpMappingsTask::class.java) {
            it.dependsOn(downloadTask.name)
            it.downloadTask = downloadTask
            it.outputBase = project.getCachedFile("mappings/mcp-${minecraftVersion()}").absolutePath
            it.channelProvider = { channel ?: "stable" }
            it.versionProvider = { version ?: "unknown" }
            it.minecraftVersionProvider = minecraftVersion
        }

        return loadName to { task.get().getOutput() }
    }

    companion object {
        private val manifestConsumers = arrayListOf<Pair<McpMappingProvider, () -> String>>()
    }
}