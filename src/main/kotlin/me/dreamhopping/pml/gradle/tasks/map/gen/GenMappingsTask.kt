package me.dreamhopping.pml.gradle.tasks.map.gen

import me.dreamhopping.pml.gradle.mappings.MappingProvider
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class GenMappingsTask : DefaultTask() {
    @Internal
    lateinit var providers: List<MappingProvider>

    @Internal
    lateinit var outputBase: String

    @Internal
    lateinit var inputProviders: List<() -> File>

    @Input
    fun getInput() = inputProviders.map { it() }

    @OutputFile
    fun getOutput() = File("$outputBase-${providers.joinToString("-") { it.id }}.json")

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun generate() {
        getWorkerExecutor().noIsolation().submit(GenMappingsAction::class.java) {
            it.inputs.set(getInput())
            it.output.set(getOutput())
        }
    }
}