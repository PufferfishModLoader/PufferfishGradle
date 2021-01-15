package me.dreamhopping.pml.gradle.tasks.download

import me.dreamhopping.pml.gradle.util.download
import org.gradle.workers.WorkAction

abstract class DownloadAction : WorkAction<DownloadParameters> {
    override fun execute() {
        download(parameters.url.get(), parameters.output.asFile.get(), sha1 = parameters.sha1.get())
    }
}