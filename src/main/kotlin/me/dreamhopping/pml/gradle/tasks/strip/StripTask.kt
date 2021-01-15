package me.dreamhopping.pml.gradle.tasks.strip

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class StripTask : DefaultTask() {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    var input: File? = null

    @Input
    var allowedDirectories = hashSetOf<String>()

    @OutputFile
    var classOutput: File? = null

    @OutputFile
    var resourceOutput: File? = null

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun strip() {
        getWorkerExecutor().noIsolation().submit(StripAction::class.java) {
            it.input.set(input)
            it.allowedDirectories.set(allowedDirectories)
            it.classOutput.set(classOutput)
            it.resourceOutput.set(resourceOutput)
        }
    }
}