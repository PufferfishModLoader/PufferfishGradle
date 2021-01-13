package me.dreamhopping.pml.gradle.tasks.map.yarn

import me.dreamhopping.pml.gradle.mappings.Mapping
import me.dreamhopping.pml.gradle.util.Json.toJson
import org.gradle.workers.WorkAction
import java.util.zip.ZipFile

abstract class LoadYarnMappingsAction : WorkAction<LoadYarnMappingsParameters> {
    override fun execute() {
        val inputJar = parameters.inputJar.asFile.get()
        val outputJson = parameters.outputJson.asFile.get()

        ZipFile(inputJar).use { input ->
            val entry = input.getEntry("mappings/mappings.tiny")
            input.getInputStream(entry).bufferedReader().use { reader ->
                val mappings = Mapping()

                reader.lines().skip(1).forEach {
                    val parts = it.split(" ", "\t")
                    when (parts[0]) {
                        "CLASS" -> mappings.ensureClassMapped(parts[1], parts[3])
                        "METHOD" -> mappings.classes[parts[1]]?.ensureMethodMapped(parts[3], parts[2], parts[5])
                        "FIELD" ->  mappings.classes[parts[1]]?.ensureFieldMapped(parts[3], parts[5])
                    }
                }

                mappings.toJson(outputJson)
            }
        }
    }
}