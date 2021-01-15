package me.dreamhopping.pml.gradle.tasks.map.generate

import me.dreamhopping.pml.gradle.data.mappings.Mappings
import me.dreamhopping.pml.gradle.data.mappings.MethodMaps
import me.dreamhopping.pml.gradle.util.toJson
import org.gradle.workers.WorkAction

abstract class GenerateMappingsAction : WorkAction<GenerateMappingsParameters> {
    override fun execute() {
        val providers = parameters.providers.get()
        val output = parameters.output.asFile.get()

        val maps = Mappings(linkedMapOf(), linkedMapOf(), linkedMapOf())

        providers.reversed().forEach { provider ->
            val m = provider.mappings

            maps.classes += m.classes
            maps.fields += m.fields

            m.methods.forEach { (key, m2) ->
                maps.methods.getOrPut(key) { MethodMaps(m2.mappedName, linkedMapOf()) }.let {
                    it.mappedName = m2.mappedName
                    it.locals += m2.locals
                }
            }
        }

        output.parentFile?.mkdirs()
        maps.toJson(output)
    }
}