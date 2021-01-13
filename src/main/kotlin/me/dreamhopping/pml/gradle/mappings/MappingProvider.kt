package me.dreamhopping.pml.gradle.mappings

import org.gradle.api.Project
import java.io.File
import java.io.Serializable

interface MappingProvider : Serializable {
    val id: String

    fun fetchIdFromDiskIfPossible(project: Project, minecraftVersion: String)

    fun setUpVersionSetupTasks(project: Project, id: String, minecraftVersion: () -> String): Pair<String, () -> File>
}