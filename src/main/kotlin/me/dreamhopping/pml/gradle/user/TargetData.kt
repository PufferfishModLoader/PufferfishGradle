package me.dreamhopping.pml.gradle.user

import me.dreamhopping.pml.gradle.mappings.MappingProvider
import me.dreamhopping.pml.gradle.mappings.McpMappingProvider
import me.dreamhopping.pml.gradle.mappings.YarnMappingProvider
import me.dreamhopping.pml.gradle.target.TargetConfigurator
import me.dreamhopping.pml.gradle.util.ModificationCallbackList
import org.gradle.api.Project
import java.io.File

@Suppress("MemberVisibilityCanBePrivate", "unused")
class TargetData(val project: Project, val version: String) {
    var runDir: File = project.file("run/$version")
    val model: UserData = project.extensions.getByType(UserData::class.java)

    var clientMainClass = model.clientRunClass
    var serverMainClass = model.serverRunClass

    val mappings = ModificationCallbackList<MappingProvider> { TargetConfigurator.refreshMcDep(project, this) }
    val accessTransformers = hashSetOf<File>()
    var clientArgs = arrayListOf<String>()
    var serverArgs = arrayListOf<String>()

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

    @JvmOverloads
    fun mcp(channel: String? = null, version: String? = null) {
        mappings.add(McpMappingProvider(channel, version, project, this.version))
    }

    @JvmOverloads
    fun yarn(version: String? = null) {
        mappings.add(YarnMappingProvider(version, project, this.version))
    }
}