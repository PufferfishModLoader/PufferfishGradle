package me.dreamhopping.pml.gradle.tasks.download

import me.dreamhopping.pml.gradle.data.AssetIndex
import me.dreamhopping.pml.gradle.util.Hash.sha1
import me.dreamhopping.pml.gradle.util.Json.fromJson
import org.gradle.workers.WorkAction
import java.io.File
import java.net.URL

abstract class DownloadAssetsAction : WorkAction<DownloadAssetsParameters> {
    override fun execute() {
        val assetIndexFile = parameters.assetIndex.asFile.get()
        val outputDirectory = parameters.outputDirectory.asFile.get()

        val assetIndex = assetIndexFile.fromJson<AssetIndex>()

        assetIndex.objects.values.forEach {
            val file = File(outputDirectory, "${it.hash.substring(0, 2)}/${it.hash}")
            // We could do up-to-date checks using task.outputs.upToDateWhen {}, but if we do it here it can be done on different threads for different versions.
            if (!file.exists() || file.sha1() != it.hash) {
                DownloadAction.download(
                    URL("http://resources.download.minecraft.net/${it.hash.substring(0, 2)}/${it.hash}"),
                    file
                )
            }
        }
    }
}