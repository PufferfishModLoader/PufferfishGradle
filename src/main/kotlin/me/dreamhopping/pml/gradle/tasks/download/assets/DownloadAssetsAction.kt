package me.dreamhopping.pml.gradle.tasks.download.assets

import me.dreamhopping.pml.gradle.data.minecraft.AssetIndex
import me.dreamhopping.pml.gradle.util.download
import me.dreamhopping.pml.gradle.util.fromJson
import org.gradle.workers.WorkAction
import java.io.File

abstract class DownloadAssetsAction : WorkAction<DownloadAssetsParameters> {
    override fun execute() {
        val assetIndex = parameters.assetIndex.asFile.get().fromJson<AssetIndex>()
        val runDir = parameters.runDir.asFile.get()
        val assetDir = parameters.assetsDir.asFile.get()

        val inOldFormat = assetIndex.virtual == true || assetIndex.mapToResources == true
        val outputDir = when {
            assetIndex.mapToResources == true -> File(runDir, "resources")
            assetIndex.virtual == true -> File(assetDir, "virtual/legacy")
            else -> File(assetDir, "objects")
        }

        for ((name, asset) in assetIndex.objects) {
            val path = "${asset.hash.substring(0, 2)}/${asset.hash}"
            val fsPath = if (inOldFormat) name else path
            val file = File(outputDir, fsPath)
            download("http://resources.download.minecraft.net/$path", file, asset.hash)
        }
    }
}