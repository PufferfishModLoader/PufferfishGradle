package me.dreamhopping.pml.gradle.tasks.download

import me.dreamhopping.pml.gradle.data.VersionManifest
import me.dreamhopping.pml.gradle.util.Json.fromJson
import org.gradle.api.tasks.*
import java.io.File
import java.net.URL

@CacheableTask
abstract class DownloadVersionJsonTask : DownloadTask() {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    lateinit var manifest: File

    @Input
    lateinit var version: String

    override var url: URL
        get() = manifest.fromJson<VersionManifest>().find(version)?.url ?: error("Invalid version $version")
        set(_) {}
}