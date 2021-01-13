package me.dreamhopping.pml.gradle.tasks.download

import me.dreamhopping.pml.gradle.data.version.VersionJson
import me.dreamhopping.pml.gradle.util.Json.fromJson
import me.dreamhopping.pml.gradle.util.getCachedFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class DownloadAssetsTask : DefaultTask() {
    @Internal
    lateinit var versionJson: File

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    fun getAssetIndex() = versionJson.takeIf { it.exists() }
        ?.let { project.getCachedFile("assets/indexes/${it.fromJson<VersionJson>().assets}.json") }
        ?: project.getCachedFile("assets/indexes/unknown.json")

    @Internal
    lateinit var outputDirectory: File

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun download() {
        getWorkerExecutor().noIsolation().submit(DownloadAssetsAction::class.java) {
            it.assetIndex.set(getAssetIndex())
            it.outputDirectory.set(outputDirectory)
        }
    }
}