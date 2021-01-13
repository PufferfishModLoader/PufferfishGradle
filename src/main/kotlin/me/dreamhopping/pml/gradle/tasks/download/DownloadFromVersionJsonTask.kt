package me.dreamhopping.pml.gradle.tasks.download

import me.dreamhopping.pml.gradle.data.version.VersionJson
import me.dreamhopping.pml.gradle.util.Json.fromJson
import org.gradle.api.tasks.*
import java.io.File
import java.net.URL

@CacheableTask
abstract class DownloadFromVersionJsonTask : DownloadTask() {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    lateinit var versionJson: File

    @Internal
    lateinit var getUrlFromJson: VersionJson.() -> URL

    override var url: URL
        get() = getUrlFromJson(versionJson.fromJson())
        set(_) {}
}