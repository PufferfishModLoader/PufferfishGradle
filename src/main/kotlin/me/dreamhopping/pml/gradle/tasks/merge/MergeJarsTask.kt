package me.dreamhopping.pml.gradle.tasks.merge

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class MergeJarsTask : DefaultTask() {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    lateinit var clientJar: File

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    lateinit var serverJar: File

    @OutputFile
    lateinit var outputJar: File

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun merge() {
        getWorkerExecutor().noIsolation().submit(MergeJarsAction::class.java) {
            it.clientJar.set(clientJar)
            it.serverJar.set(serverJar)
            it.outputJar.set(outputJar)
        }
    }
}