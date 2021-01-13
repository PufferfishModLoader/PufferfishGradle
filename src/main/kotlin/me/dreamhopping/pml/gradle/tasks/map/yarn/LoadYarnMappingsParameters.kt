package me.dreamhopping.pml.gradle.tasks.map.yarn

import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkParameters

interface LoadYarnMappingsParameters : WorkParameters {
    val inputJar: RegularFileProperty
    val outputJson: RegularFileProperty
}