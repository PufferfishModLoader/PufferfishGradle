package me.dreamhopping.pml.gradle.targets

import groovy.lang.Closure
import me.dreamhopping.pml.gradle.PGExtension
import me.dreamhopping.pml.gradle.mappings.CustomMappingProvider
import me.dreamhopping.pml.gradle.mappings.MappingProvider
import me.dreamhopping.pml.gradle.mappings.YarnMappingProvider
import me.dreamhopping.pml.gradle.mappings.YarnMappingProvider.Companion.isYarnAvailable
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import java.io.File

@Suppress("UNUSED")
class TargetExtension(val minecraft: PGExtension, val project: Project, val version: String, addDefaultMappings: Boolean = true) {
    val mappingProviders = arrayListOf<MappingProvider>()
    var mappingVersion = version
    var accessTransformers = hashSetOf<String>()
    var clientMainClass = "me.dreamhopping.pml.launch.PMLClientMain"
    var serverMainClass = "me.dreamhopping.pml.launch.PMLServerMain"
    var runDir = File(project.projectDir, "run/$version")
    var sourceSetName = "mc$version"
        set(v) {
            val old = field
            field = v
            TargetConfig.onSourceSetNameChange(this, old)
        }

    init {
        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.maybeCreate(sourceSetName)
        TargetConfig.setupTarget(this)

        if (addDefaultMappings) {
            if (project.isYarnAvailable(version)) {
                yarn()
            }
        }
    }

    fun clientMainClass(name: String) {
        clientMainClass = name
    }

    fun serverMainClass(name: String) {
        serverMainClass = name
    }

    fun sourceSetName(name: String) {
        sourceSetName = name
    }

    fun mappingVersion(version: String) {
        mappingVersion = version
    }

    @JvmOverloads
    fun yarn(version: String? = null) {
        mappingProviders.add(YarnMappingProvider(version) { onIdChange() })
        onIdChange()
    }

    private fun onIdChange() {
        TargetConfig.onIdChange(project, version, mappingProviders.joinToString("-") { it.id })
    }

    fun mappings(closure: Closure<*>) {
        val provider = CustomMappingProvider(project)
        project.configure(provider, closure)
        mappingProviders.add(provider)
        onIdChange()
    }
}
