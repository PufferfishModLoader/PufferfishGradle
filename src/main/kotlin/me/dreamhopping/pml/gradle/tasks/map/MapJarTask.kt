package me.dreamhopping.pml.gradle.tasks.map

import me.dreamhopping.pml.gradle.mappings.MappingProvider
import me.dreamhopping.pml.gradle.tasks.map.gen.GenMappingsTask
import me.dreamhopping.pml.gradle.util.getRepoFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class MapJarTask : DefaultTask() {
    @Internal
    lateinit var providers: List<MappingProvider>

    @Internal
    lateinit var version: String

    @Internal
    lateinit var genTask: TaskProvider<GenMappingsTask>

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    lateinit var input: File

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    fun getMappings() = genTask.get().getOutput()

    @OutputFile
    fun getOutput() = project.getRepoFile("net.minecraft", "merged", "$version-${providers.joinToString("-") { it.id }}")

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun map() {
        getWorkerExecutor().noIsolation().submit(MapJarAction::class.java) {
            it.input.set(input)
            it.mappings.set(getMappings())
            it.output.set(getOutput())
        }
    }
}