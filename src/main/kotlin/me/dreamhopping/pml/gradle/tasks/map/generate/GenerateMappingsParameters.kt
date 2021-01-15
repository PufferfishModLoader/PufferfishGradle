package me.dreamhopping.pml.gradle.tasks.map.generate

import me.dreamhopping.pml.gradle.mappings.MappingProvider
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkParameters

interface GenerateMappingsParameters : WorkParameters {
    val providers: ListProperty<MappingProvider>
    val output: RegularFileProperty
}