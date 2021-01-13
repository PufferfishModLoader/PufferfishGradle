package me.dreamhopping.pml.gradle.tasks.map.yarn

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class LoadYarnMappingsTask : DefaultTask() {
    @Internal
    lateinit var downloadTask: DownloadYarnMappingsTask

    @Internal
    lateinit var versionProvider: () -> String

    @Internal
    lateinit var outputBase: String

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    fun getInputJar() = downloadTask.output

    @OutputFile
    fun getOutputJson() = File("$outputBase-${versionProvider()}.json")

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun load() {
        getWorkerExecutor().noIsolation().submit(LoadYarnMappingsAction::class.java) {
            it.inputJar.set(getInputJar())
            it.outputJson.set(getOutputJson())
        }
    }
}