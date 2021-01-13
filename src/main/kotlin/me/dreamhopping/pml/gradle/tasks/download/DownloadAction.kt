package me.dreamhopping.pml.gradle.tasks.download

import org.gradle.workers.WorkAction
import java.io.File
import java.net.URL

abstract class DownloadAction : WorkAction<DownloadParameters> {
    override fun execute() {
        val output = parameters.output.asFile.get()
        val url = parameters.url.get()

        download(url, output)
    }

    companion object {
        fun download(url: URL, output: File) {
            output.parentFile?.mkdirs()
            url.openConnection().apply {
                setRequestProperty(
                    "User-Agent",
                    "PufferfishGradle/${DownloadTask::class.java.`package`.implementationVersion}"
                )
                getInputStream().use { output.outputStream().use { out -> it.copyTo(out) } }
            }
        }
    }
}