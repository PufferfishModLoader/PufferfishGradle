package me.dreamhopping.pml.gradle.tasks.map.mcp

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters

interface LoadMcpMappingsParameters : WorkParameters {
    val mcp: RegularFileProperty
    val srg: RegularFileProperty
    val output: RegularFileProperty
    val legacy: Property<Boolean>
}