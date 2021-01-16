package me.dreamhopping.pml.gradle.tasks.download.assets

import me.dreamhopping.pml.gradle.util.dataFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class DownloadAssetsTask : DefaultTask() {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    var assetIndex: File? = null

    @Internal
    var runDir: File? = null

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun download() {
        getWorkerExecutor().noIsolation().submit(DownloadAssetsAction::class.java) {
            it.assetsDir.set(project.dataFile("assets"))
            it.runDir.set(runDir)
            it.assetIndex.set(assetIndex)
        }
    }
}