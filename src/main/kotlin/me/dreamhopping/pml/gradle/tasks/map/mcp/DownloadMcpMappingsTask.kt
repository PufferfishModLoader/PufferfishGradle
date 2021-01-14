package me.dreamhopping.pml.gradle.tasks.map.mcp

import me.dreamhopping.pml.gradle.tasks.download.DownloadAction
import me.dreamhopping.pml.gradle.util.getRepoFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.net.URL
import javax.inject.Inject

@CacheableTask
abstract class DownloadMcpMappingsTask : DefaultTask() {
    @Internal
    lateinit var getVersion: () -> String

    @Internal
    lateinit var getChannel: () -> String

    @Internal
    lateinit var getMinecraftVersion: () -> String

    @Input
    fun getMcpURL() = URL(getVersion().let { "https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_${getChannel()}/${getVersion()}/mcp_${getChannel()}-${getVersion()}.zip" })

    @Input
    fun getSrgURL() = URL(
        if (isMc13()) {
            "https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/${getMinecraftVersion()}/mcp_config-${getMinecraftVersion()}.zip"
        } else {
            "https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp/${getMinecraftVersion()}/mcp-${getMinecraftVersion()}-srg.zip"
        }
    )

    @OutputFile
    fun getMcpOutput() = project.getRepoFile("de.oceanlabs.mcp", "mcp_${getChannel()}", getVersion(), extension = "zip")

    @OutputFile
    fun getSrgOutput() = project.getRepoFile(
        "de.oceanlabs.mcp",
        if (isMc13()) "mcp_config" else "mcp",
        getMinecraftVersion(),
        classifier = "srg".takeUnless { isMc13() },
        extension = "zip"
    )

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun download() {
        getWorkerExecutor().noIsolation().submit(DownloadAction::class.java) {
            it.url.set(getMcpURL())
            it.output.set(getMcpOutput())
        }
        getWorkerExecutor().noIsolation().submit(DownloadAction::class.java) {
            it.url.set(getSrgURL())
            it.output.set(getSrgOutput())
        }
    }

    private fun isMc13() = getMinecraftVersion().split(".")[1].toInt() >= 13
}