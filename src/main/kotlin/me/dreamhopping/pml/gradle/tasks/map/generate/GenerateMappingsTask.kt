package me.dreamhopping.pml.gradle.tasks.map.generate

import me.dreamhopping.pml.gradle.mappings.MappingProvider
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class GenerateMappingsTask : DefaultTask() {
    @Input
    var mappingProviders: List<MappingProvider>? = null

    @OutputFile
    var outputFile: File? = null

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun generate() {
        getWorkerExecutor().noIsolation().submit(GenerateMappingsAction::class.java) {
            it.output.set(outputFile)
            it.providers.set(mappingProviders)
        }
    }
}