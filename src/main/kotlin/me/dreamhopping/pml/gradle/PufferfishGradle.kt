package me.dreamhopping.pml.gradle

import me.dreamhopping.pml.gradle.user.UserData
import org.gradle.api.Plugin
import org.gradle.api.Project

class PufferfishGradle : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyPlugin("java")
        target.applyPlugin("idea")

        target.extensions.add("minecraft", UserData(target))
    }

    private fun Project.applyPlugin(id: String) {
        apply(mapOf("plugin" to id))
    }
}