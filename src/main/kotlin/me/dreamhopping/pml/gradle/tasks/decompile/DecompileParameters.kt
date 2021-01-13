package me.dreamhopping.pml.gradle.tasks.decompile

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkParameters
import java.io.File

interface DecompileParameters : WorkParameters {
    val input: RegularFileProperty
    val output: RegularFileProperty
    val libraries: ListProperty<File>
    val mappings: RegularFileProperty
}