package me.dreamhopping.pml.gradle.user

import me.dreamhopping.pml.gradle.mappings.DebugMappingProvider
import me.dreamhopping.pml.gradle.mappings.MappingProvider
import me.dreamhopping.pml.gradle.target.TargetConfigurator
import me.dreamhopping.pml.gradle.util.ModificationCallbackList
import org.gradle.api.Project
import java.io.File

@Suppress("MemberVisibilityCanBePrivate", "unused")
class TargetData(val project: Project, val version: String) {
    val mappings = ModificationCallbackList<MappingProvider> { TargetConfigurator.refreshMcDep(project, this) }
    val accessTransformers = hashSetOf<File>()

    init {
        mappings.add(DebugMappingProvider)
    }

    fun accessTransformer(file: Any) {
        accessTransformers.add(project.file(file).absoluteFile)
    }
}