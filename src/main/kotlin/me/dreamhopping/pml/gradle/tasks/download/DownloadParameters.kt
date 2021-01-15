package me.dreamhopping.pml.gradle.tasks.download

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters

interface DownloadParameters : WorkParameters {
    val url: Property<String>
    val sha1: Property<String?>
    val output: RegularFileProperty
}