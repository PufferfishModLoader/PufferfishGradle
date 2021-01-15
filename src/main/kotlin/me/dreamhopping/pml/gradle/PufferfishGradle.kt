package me.dreamhopping.pml.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class PufferfishGradle : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyPlugin("java")
        target.applyPlugin("idea")
    }

    private fun Project.applyPlugin(id: String) {
        apply(mapOf("plugin" to id))
    }
}