package me.dreamhopping.pml.gradle.tasks.map.yarn

import me.dreamhopping.pml.gradle.data.yarn.YarnManifestEntry
import me.dreamhopping.pml.gradle.tasks.download.DownloadTask
import me.dreamhopping.pml.gradle.util.Json.fromJson
import me.dreamhopping.pml.gradle.util.getRepoFile
import org.gradle.api.tasks.*
import java.io.File
import java.net.URL

@CacheableTask
abstract class DownloadYarnMappingsTask : DownloadTask() {
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    var manifest: File? = null

    @Input
    @Optional
    var versionOverride: String? = null

    override var url: URL
        get() = URL(getVersion().let { "https://maven.fabricmc.net/net/fabricmc/yarn/$it/yarn-$it.jar" })
        set(_) {}

    override var output: File
        get() = project.getRepoFile("net.fabricmc", "yarn", getVersion())
        set(_) {}

    private fun getVersion() = versionOverride ?: manifest?.takeIf { it.exists() }?.fromJson<List<YarnManifestEntry>>()
        ?.firstOrNull()?.mappings?.version ?: "unknown"
}