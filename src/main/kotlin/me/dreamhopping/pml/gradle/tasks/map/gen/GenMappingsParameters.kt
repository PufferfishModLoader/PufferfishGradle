package me.dreamhopping.pml.gradle.tasks.map.gen

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkParameters
import java.io.File

interface GenMappingsParameters : WorkParameters {
    val inputs: ListProperty<File>
    val output: RegularFileProperty
}