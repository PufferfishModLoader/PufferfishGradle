package me.dreamhopping.pml.gradle.tasks.merge

import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkParameters

interface MergeParameters : WorkParameters {
    val clientJar: RegularFileProperty
    val serverJar: RegularFileProperty
    val outputJar: RegularFileProperty
}