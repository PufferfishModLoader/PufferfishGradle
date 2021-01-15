package me.dreamhopping.pml.gradle

import me.dreamhopping.pml.gradle.user.UserData
import me.dreamhopping.pml.gradle.util.repoDir
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

class PufferfishGradle : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyPlugin("java")
        target.applyPlugin("idea")

        target.repositories.maven {
            it.setUrl(target.repoDir.toURI().toURL())
            if (GradleVersion.current() >= GradleVersion.version("6.0")) {
                it.metadataSources { sources ->
                    sources.artifact()
                }
            }
        }

        target.extensions.add("minecraft", UserData(target))
    }

    private fun Project.applyPlugin(id: String) {
        apply(mapOf("plugin" to id))
    }
}