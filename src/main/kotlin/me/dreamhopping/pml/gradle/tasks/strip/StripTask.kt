package me.dreamhopping.pml.gradle.tasks.strip

import me.dreamhopping.pml.gradle.tasks.download.DownloadTask
import me.dreamhopping.pml.gradle.util.repoFile
import org.gradle.api.DefaultTask
import org.gradle.api.Project
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

    companion object {
        fun register(
            name: String,
            artifact: String,
            version: String,
            project: Project,
            downloadTask: TaskProvider<DownloadTask>
        ): TaskProvider<StripTask> = project.tasks.register(name, StripTask::class.java) {
            it.dependsOn(downloadTask.name)
            it.input = downloadTask.get().output
            it.allowedDirectories = hashSetOf("net/minecraft", "com/mojang/rubydung")
            it.classOutput = project.repoFile("net.minecraft", artifact, version, "classes")
            it.resourceOutput = project.repoFile("net.minecraft", artifact, version, "resources")
        }
    }
}