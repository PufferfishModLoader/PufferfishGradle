package me.dreamhopping.pml.gradle.tasks.merge

import me.dreamhopping.pml.gradle.tasks.strip.StripTask
import me.dreamhopping.pml.gradle.util.repoFile
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class MergeTask : DefaultTask() {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    var clientJar: File? = null

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    var serverJar: File? = null

    @OutputFile
    var outputJar: File? = null

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun merge() {
        getWorkerExecutor().noIsolation().submit(MergeAction::class.java) {
            it.clientJar.set(clientJar)
            it.serverJar.set(serverJar)
            it.outputJar.set(outputJar)
        }
    }

    companion object {
        fun register(
            project: Project,
            name: String,
            artifact: String,
            version: String,
            client: () -> File?,
            server: () -> File?
        ): TaskProvider<MergeTask> = project.tasks.register(name, MergeTask::class.java) {
            it.dependsOn()
            it.clientJar = client()
            it.serverJar = server()
            it.outputJar = project.repoFile("net.minecraft", artifact, version)
        }
    }
}