package me.dreamhopping.pml.gradle.tasks.download

import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkParameters

interface DownloadAssetsParameters : WorkParameters {
    val assetIndex: RegularFileProperty
    val outputDirectory: RegularFileProperty
}