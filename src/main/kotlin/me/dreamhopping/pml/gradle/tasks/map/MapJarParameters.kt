package me.dreamhopping.pml.gradle.tasks.map

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkParameters
import java.io.File

interface MapJarParameters : WorkParameters {
    val input: RegularFileProperty
    val mappings: RegularFileProperty
    val output: RegularFileProperty
    val inheritanceOutput: RegularFileProperty
    val accessTransformers: ListProperty<File>
}