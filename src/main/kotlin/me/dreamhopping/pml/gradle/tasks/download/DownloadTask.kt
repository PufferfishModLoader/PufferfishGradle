package me.dreamhopping.pml.gradle.tasks.download

import me.dreamhopping.pml.gradle.util.sha1
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class DownloadTask : DefaultTask() {
    @Input
    var url: String? = null

    @Input
    @Optional
    var sha1: String? = null

    @OutputFile
    var output: File? = null

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    init {
        outputs.upToDateWhen {
            output!!.let { it.exists() && (sha1 == null || it.sha1() == sha1) }
        }
    }

    @TaskAction
    fun download() {
        getWorkerExecutor().noIsolation().submit(DownloadAction::class.java) {
            it.url.set(url)
            it.sha1.set(sha1)
            it.output.set(output)
        }
    }
}