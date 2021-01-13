package me.dreamhopping.pml.gradle.tasks.strip

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters

interface StripJarParameters : WorkParameters {
    val input: RegularFileProperty
    val classOutput: RegularFileProperty
    val resourceOutput: RegularFileProperty
    val allowedDirectories: ListProperty<String>
}