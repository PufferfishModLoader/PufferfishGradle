package me.dreamhopping.pml.gradle.tasks.download

import me.dreamhopping.pml.gradle.data.version.VersionJson
import me.dreamhopping.pml.gradle.util.Json.fromJson
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class AddLibrariesTask : DefaultTask() {
    @InputFile
    lateinit var versionJson: File

    @Internal
    lateinit var configuration: Configuration

    @Internal
    lateinit var nativesConfiguration: Configuration

    @TaskAction
    fun download() {
        versionJson.fromJson<VersionJson>().libraries.forEach {
            if (it.isAllowed()) {
                if (it.natives == null) {
                    project.dependencies.add(configuration.name, it.name)
                } else {
                    project.dependencies.add(nativesConfiguration.name, "${it.name}:${it.getNative()}")
                }
            }
        }
    }
}