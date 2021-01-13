package me.dreamhopping.pml.gradle.tasks.map

import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkParameters

interface MapJarParameters : WorkParameters {
    val input: RegularFileProperty
    val mappings: RegularFileProperty
    val output: RegularFileProperty
    val inheritanceOutput: RegularFileProperty
}