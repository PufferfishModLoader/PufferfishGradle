package me.dreamhopping.pml.gradle.mods

import me.dreamhopping.pml.gradle.PGExtension
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import java.io.Serializable

class ModExtension(val minecraft: PGExtension, val name: String) : Serializable {
    class Factory(private val ext: PGExtension, private val project: Project) : NamedDomainObjectFactory<ModExtension> {
        override fun create(name: String) = ModExtension(ext, name).also {
            createConfig(name)
            ModConfig.setUpModExt(it, project)
        }

        private fun createConfig(name: String) = project.configurations.create("${name}Library")
    }
}