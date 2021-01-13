package me.dreamhopping.pml.gradle.tasks.run

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import java.io.File

interface ExtractNativesParameters : WorkParameters {
    val files: ListProperty<File>
    val outputDir: RegularFileProperty
    val pattern: Property<String>
}