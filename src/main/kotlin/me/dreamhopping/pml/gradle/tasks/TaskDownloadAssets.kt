package me.dreamhopping.pml.gradle.tasks

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import me.dreamhopping.pml.gradle.mc.data.assets.AssetIndex
import me.dreamhopping.pml.gradle.utils.Hash
import me.dreamhopping.pml.gradle.utils.http.Downloader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskDownloadAssets : DefaultTask() {
    @Input
    lateinit var index: AssetIndex
    @Input
    lateinit var output: String

    init {
        outputs.upToDateWhen { _ ->
            index.objects.any {
                val file = File(output, "objects/${it.value.hash.substring(0, 2)}/${it.value.hash}")
                file.exists() && Hash.sha1(file) == it.value.hash
            }
        }
    }

    @TaskAction
    fun download() {
        runBlocking {
            index.objects.map {
                val path = "${it.value.hash.substring(0, 2)}/${it.value.hash}"
                val file = File(output, "objects/$path")
                async { Downloader.download("http://resources.download.minecraft.net/$path", file, it.value.hash, silent = true) }
            }.awaitAll()
        }
    }
}