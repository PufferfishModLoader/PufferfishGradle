package me.dreamhopping.pml.gradle.tasks.map

import me.dreamhopping.pml.gradle.mappings.MappingProvider
import me.dreamhopping.pml.gradle.tasks.map.gen.GenMappingsTask
import me.dreamhopping.pml.gradle.util.Hash.sha1
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

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    lateinit var accessTransformers: Set<String>

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    lateinit var input: File

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    fun getMappings() = genTask.get().getOutput()

    @OutputFile
    fun getOutput() = project.getRepoFile("net.minecraft", "merged", getVersion(version, providers, accessTransformers))

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun map() {
        getWorkerExecutor().noIsolation().submit(MapJarAction::class.java) { params ->
            params.input.set(input)
            params.accessTransformers.set(accessTransformers.map { File(it) })
            params.mappings.set(getMappings())
            params.output.set(getOutput())
        }
    }

    companion object {
        fun getVersion(id: String, providers: Collection<MappingProvider>, accessTransformers: Set<String>) =
            "$id-${providers.joinToString("-") { it.id }}-${
                accessTransformers.joinToString("-") { File(it).sha1() }.takeIf { it.isNotEmpty() } ?: "none"
            }"
    }
}