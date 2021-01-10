package me.dreamhopping.pml.gradle.data

import groovy.lang.Closure
import me.dreamhopping.pml.gradle.mappings.MappingProvider
import me.dreamhopping.pml.gradle.mappings.YarnMappingProvider
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import java.io.File

class TargetExt(val project: Project, val version: String) {
    var mappingVersion = version
    val accessTransformers = hashSetOf<String>()
    var clientMainClass = "me.dreamhopping.pml.launch.PMLClientMain"
    var serverMainClass = "me.dreamhopping.pml.launch.PMLServerMain"
    var runDir = File(project.projectDir, "run/$version")
    lateinit var mappings: MappingProvider

    init {
        project.configurations.maybeCreate(getMcLibConfigName(version))
        project.configurations.maybeCreate(getMcLibNativesConfigName(version))
        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.maybeCreate(getMcSourceSetName(version))

        if (YarnMappingProvider.isAvailable(project, version)) {
            yarn()
        } else {
            mcp()
        }
    }

    fun mcp() = mappings("mcp")
    fun mcp(closure: Closure<*>) = mappings("mcp", closure)
    fun yarn() = mappings("yarn")
    fun yarn(closure: Closure<*>) = mappings("yarn", closure)

    fun accessTransformer(vararg transformers: String) {
        accessTransformers += transformers
    }

    fun clientMainClass(name: String) {
        clientMainClass = name
    }

    fun serverMainClass(name: String) {
        serverMainClass = name
    }

    fun mappingVersion(version: String) {
        mappingVersion = version
    }

    fun runDir(dir: Any) {
        runDir = project.file(dir)
    }

    fun mappings(name: String) {
        mappings(name) {}
    }

    fun mappings(name: String, closure: Closure<*>) {
        val provider = MappingProvider[name]
        project.configure(setOf(provider), closure)
        mappings = provider
    }

    fun mappings(name: String, closure: Any.() -> Unit) {
        val provider = MappingProvider[name]
        project.configure(setOf(provider), closure)
        mappings = provider
    }

    companion object {
        fun getMcLibConfigName(version: String) = "pgMcLibs$version"
        fun getMcLibNativesConfigName(version: String) = "pgMcNatives$version"
        fun getMcSourceSetName(version: String) = "mc$version"
    }
}