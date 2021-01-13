package me.dreamhopping.pml.gradle.tasks.download

import me.dreamhopping.pml.gradle.util.Hash
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.net.URL
import javax.inject.Inject

@CacheableTask
abstract class DownloadTask : DefaultTask() {
    @Input
    open lateinit var url: URL

    @Input
    @Optional
    var sha1: String? = null

    @Input
    var downloadEvenIfNotNecessary = false

    @OutputFile
    open lateinit var output: File

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    init {
        outputs.upToDateWhen {
            !downloadEvenIfNotNecessary && output.exists() && (sha1 == null || output.inputStream()
                .use { Hash.sha1(it) } == sha1)
        }

        onlyIf { !project.gradle.startParameter.isOffline }
    }

    @TaskAction
    fun download() {
        val queue = getWorkerExecutor().noIsolation()

        queue.submit(DownloadAction::class.java) {
            it.output.set(output)
            it.url.set(url)
        }
    }
}