package me.dreamhopping.pml.gradle.tasks.decompile

import me.dreamhopping.pml.gradle.mappings.MappingProvider
import me.dreamhopping.pml.gradle.tasks.map.MapJarTask
import me.dreamhopping.pml.gradle.tasks.map.gen.GenMappingsTask
import me.dreamhopping.pml.gradle.util.getRepoFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class DecompileTask : DefaultTask() {
    @Internal
    lateinit var providers: List<MappingProvider>

    @Internal
    lateinit var version: String

    @Internal
    lateinit var mapJarTask: TaskProvider<MapJarTask>

    @Internal
    lateinit var genTask: TaskProvider<GenMappingsTask>

    @Internal
    lateinit var accessTransformers: Set<String>

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    fun getInput() = mapJarTask.get().getOutput()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    lateinit var fernflower: File

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    fun getMappings() = genTask.get().getOutput()

    @Classpath
    lateinit var libraries: FileCollection

    @OutputFile
    fun getOutput() = project.getRepoFile(
        "net.minecraft",
        "merged",
        MapJarTask.getVersion(version, providers, accessTransformers),
        "sources"
    )

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun decompile() {
        val queue = getWorkerExecutor().processIsolation {
            it.classpath.from(fernflower)
            it.forkOptions.jvmArgs("-Xmx3G")
        }

        synchronized(decompileLock) {
            queue.submit(DecompileAction::class.java) {
                it.input.set(getInput())
                it.libraries.set(libraries)
                it.output.set(getOutput())
                it.mappings.set(getMappings())
            }
        }
    }

    companion object {
        private val decompileLock = Object()
    }
}