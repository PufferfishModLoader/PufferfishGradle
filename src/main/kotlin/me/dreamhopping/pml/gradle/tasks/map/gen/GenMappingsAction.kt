package me.dreamhopping.pml.gradle.tasks.map.gen

import me.dreamhopping.pml.gradle.mappings.Mapping
import me.dreamhopping.pml.gradle.util.Json.fromJson
import me.dreamhopping.pml.gradle.util.Json.toJson
import org.gradle.workers.WorkAction

abstract class GenMappingsAction : WorkAction<GenMappingsParameters> {
    override fun execute() {
        val inputs = parameters.inputs.get()
        val output = parameters.output.asFile.get()

        output.parentFile?.mkdirs()

        val mapping = Mapping()

        inputs.reversed().forEach {
            mapping.loadFrom(it.fromJson())
        }

        mapping.toJson(output)
    }
}