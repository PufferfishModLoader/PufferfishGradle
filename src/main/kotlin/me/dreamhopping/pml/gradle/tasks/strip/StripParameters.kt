package me.dreamhopping.pml.gradle.tasks.strip

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkParameters

interface StripParameters : WorkParameters {
    val allowedDirectories: SetProperty<String>
    val input: RegularFileProperty
    val classOutput: RegularFileProperty
    val resourceOutput: RegularFileProperty
}