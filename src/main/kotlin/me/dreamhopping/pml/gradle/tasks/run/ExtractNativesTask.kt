package me.dreamhopping.pml.gradle.tasks.run

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class ExtractNativesTask : DefaultTask() {
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    lateinit var config: Configuration

    @OutputDirectory
    lateinit var outputDir: File

    @Input
    lateinit var pattern: String

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun extract() {
        getWorkerExecutor().noIsolation().submit(ExtractNativesAction::class.java) {
            it.outputDir.set(outputDir)
            it.files.set(config)
            it.pattern.set(pattern)
        }
    }
}