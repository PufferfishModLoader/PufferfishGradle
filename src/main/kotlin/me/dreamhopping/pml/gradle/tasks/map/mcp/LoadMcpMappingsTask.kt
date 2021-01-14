package me.dreamhopping.pml.gradle.tasks.map.mcp

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class LoadMcpMappingsTask : DefaultTask() {
    @Internal
    lateinit var downloadTask: TaskProvider<DownloadMcpMappingsTask>
    @Internal
    lateinit var outputBase: String
    @Internal
    lateinit var channelProvider: () -> String
    @Internal
    lateinit var versionProvider: () -> String
    @Internal
    lateinit var minecraftVersionProvider: () -> String

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    fun getMcpInput() = downloadTask.get().getMcpOutput()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    fun getSrgInput() = downloadTask.get().getSrgOutput()

    @OutputFile
    fun getOutput() = File("$outputBase-${channelProvider()}-${versionProvider()}.json")

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun load() {
        getWorkerExecutor().noIsolation().submit(LoadMcpMappingsAction::class.java) {
            it.mcp.set(getMcpInput())
            it.srg.set(getSrgInput())
            it.output.set(getOutput())
            it.legacy.set(!isMc13())
        }
    }

    private fun isMc13() = minecraftVersionProvider().split(".")[1].toInt() >= 13
}