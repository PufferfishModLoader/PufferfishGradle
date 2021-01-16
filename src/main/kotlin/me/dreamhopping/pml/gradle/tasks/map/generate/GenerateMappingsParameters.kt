package me.dreamhopping.pml.gradle.tasks.map.generate

import me.dreamhopping.pml.gradle.data.mappings.Mappings
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkParameters

interface GenerateMappingsParameters : WorkParameters {
    val mappings: ListProperty<Mappings>
    val output: RegularFileProperty
}