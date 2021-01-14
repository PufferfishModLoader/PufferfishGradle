package me.dreamhopping.pml.gradle

import me.dreamhopping.pml.gradle.mods.ModConfig
import me.dreamhopping.pml.gradle.targets.TargetConfig
import me.dreamhopping.pml.gradle.util.EXTENSION_NAME
import org.gradle.api.Plugin
import org.gradle.api.Project

class PufferfishGradle : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyPlugin("java")
        target.applyPlugin("idea")

        val ext = PGExtension(target)
        target.extensions.add(EXTENSION_NAME, ext)

        TargetConfig.setup(ext)
        ModConfig.setup(ext)
    }

    private fun Project.applyPlugin(id: String) {
        apply(mapOf("plugin" to id))
    }
}