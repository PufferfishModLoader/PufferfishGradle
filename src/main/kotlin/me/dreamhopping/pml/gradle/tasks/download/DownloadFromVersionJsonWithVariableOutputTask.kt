package me.dreamhopping.pml.gradle.tasks.download

import me.dreamhopping.pml.gradle.data.version.VersionJson
import me.dreamhopping.pml.gradle.util.Json.fromJson
import org.gradle.api.tasks.Internal
import java.io.File

abstract class DownloadFromVersionJsonWithVariableOutputTask : DownloadFromVersionJsonTask() {
    @Internal
    lateinit var getOutput: VersionJson.() -> File

    override var output: File
        get() = getOutput(versionJson.fromJson())
        set(_) {}
}