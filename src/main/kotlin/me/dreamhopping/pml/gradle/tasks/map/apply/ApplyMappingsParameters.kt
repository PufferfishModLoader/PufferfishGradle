package me.dreamhopping.pml.gradle.tasks.map.apply

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkParameters
import java.io.File

interface ApplyMappingsParameters : WorkParameters {
    val inputJar: RegularFileProperty
    val accessTransformers: SetProperty<File>
    val mappings: RegularFileProperty
    val outputJar: RegularFileProperty
}