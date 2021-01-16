package me.dreamhopping.pml.gradle.tasks.map.generate

import me.dreamhopping.pml.gradle.data.mappings.Mappings
import me.dreamhopping.pml.gradle.util.toJson
import org.gradle.workers.WorkAction

abstract class GenerateMappingsAction : WorkAction<GenerateMappingsParameters> {
    override fun execute() {
        val mappings = parameters.mappings.get()
        val output = parameters.output.asFile.get()

        val maps = Mappings(linkedMapOf(), linkedMapOf(), linkedMapOf(), linkedMapOf())

        mappings.reversed().forEach {
            maps.classes += it.classes
            maps.fields += it.fields
            maps.methods += it.methods
            maps.locals += it.locals
        }

        output.parentFile?.mkdirs()
        maps.toJson(output)
    }
}