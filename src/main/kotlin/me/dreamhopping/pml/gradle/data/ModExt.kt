package me.dreamhopping.pml.gradle.data

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import java.io.Serializable

class ModExt(val name: String, var version: String) : Serializable {
    fun version(version: String) {
        this.version = version
    }

    class Factory(private val project: Project) : NamedDomainObjectFactory<ModExt> {
        override fun create(name: String) = ModExt(name, project.version.toString()).also { createConfig(name) }

        private fun createConfig(name: String) = project.configurations.create("${name}Library")
    }
}