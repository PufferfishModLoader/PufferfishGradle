package me.dreamhopping.pml.gradle.mappings

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

abstract class MappingProvider {
    private var infoCache: MappingInfo? = null

    abstract suspend fun addDataToConfiguration(project: Project, configuration: Configuration, requestedVersion: String)

    fun load(project: Project, config: Configuration, requestedVersion: String) =
        infoCache ?: loadIgnoringCache(project, config, requestedVersion).also {
            infoCache = it
        }

    abstract suspend fun createId(project: Project, requestedVersion: String): String
    abstract fun loadIgnoringCache(project: Project, config: Configuration, requestedVersion: String): MappingInfo

    companion object {
        private val registry = hashMapOf<String, () -> MappingProvider>()

        operator fun get(name: String) = (registry[name] ?: error("Invalid mapping provider '$name'"))()

        init {
            registry["mcp"] = { McpMappingProvider() }
            registry["yarn"] = { YarnMappingProvider() }
        }
    }
}