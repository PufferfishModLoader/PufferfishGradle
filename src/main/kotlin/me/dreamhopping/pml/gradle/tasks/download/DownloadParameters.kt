package me.dreamhopping.pml.gradle.tasks.download

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import java.net.URL

interface DownloadParameters : WorkParameters {
    val url: Property<URL>
    val output: RegularFileProperty
}