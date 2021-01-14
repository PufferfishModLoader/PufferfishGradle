package me.dreamhopping.pml.gradle.mods

import me.dreamhopping.pml.gradle.PGExtension
import me.dreamhopping.pml.gradle.tasks.mods.GenModsJsonTask
import me.dreamhopping.pml.gradle.util.cast
import org.gradle.api.Project
import org.gradle.language.jvm.tasks.ProcessResources

object ModConfig {
    fun setUpModExt(ext: ModExtension, project: Project) {
        project.afterEvaluate {
            val config = it.configurations.getByName("${ext.name}Library")
            val set = ext.minecraft.mainSourceSet
            project.configurations.getByName(set.compileOnlyConfigurationName).extendsFrom(config)
        }
    }

    fun setup(ext: PGExtension) {
        val project = ext.project
        val task = project.tasks.register("genModsJson", GenModsJsonTask::class.java) {
            it.mods = ext.modContainer
        }
        project.afterEvaluate {
            val t = it.tasks.getByName(ext.mainSourceSet.processResourcesTaskName).cast<ProcessResources>()
            t.dependsOn(task.name)
            t.from(task.get().outputFile)
        }
    }
}