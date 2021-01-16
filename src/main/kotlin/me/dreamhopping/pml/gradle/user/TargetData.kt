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
    var runDir: File = project.file("run/$version")
    var clientMainClass = "me.dreamhopping.pml.main.PMLClientMain"
    var serverMainClass = "me.dreamhopping.pml.main.PMLServerMain"
    var clientArgs = arrayListOf<String>()
    var serverArgs = arrayListOf<String>()

    init {
        mappings.add(DebugMappingProvider)
    }

    fun accessTransformer(file: Any) {
        accessTransformers.add(project.file(file).absoluteFile)
    }

    fun runDir(file: Any) {
        runDir = project.file(file)
    }

    fun clientMainClass(name: String) {
        clientMainClass = name
    }

    fun serverMainClass(name: String) {
        serverMainClass = name
    }

    fun clientArgs(vararg args: String) {
        clientArgs.addAll(args)
    }

    fun serverArgs(vararg args: String) {
        serverArgs.addAll(args)
    }
}