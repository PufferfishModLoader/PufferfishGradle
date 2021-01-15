package me.dreamhopping.pml.gradle.mappings

import me.dreamhopping.pml.gradle.data.mappings.Mappings
import org.gradle.api.Project
import java.io.Serializable

interface MappingProvider : Serializable {
    val id: String
    val mappings: Mappings

    fun load(project: Project, minecraftVersion: String)
}