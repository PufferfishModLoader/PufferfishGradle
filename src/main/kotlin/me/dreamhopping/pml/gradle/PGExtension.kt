package me.dreamhopping.pml.gradle

import groovy.lang.Closure
import me.dreamhopping.pml.gradle.mods.ModExtension
import me.dreamhopping.pml.gradle.targets.TargetConfig
import me.dreamhopping.pml.gradle.targets.TargetExtension
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

class PGExtension(val project: Project) {
    val modContainer: NamedDomainObjectContainer<ModExtension> = project.container(ModExtension::class.java, ModExtension.Factory(this, project))
    var separateVersionJars = false
    val targets = arrayListOf<TargetExtension>()
    var mainSourceSet: SourceSet =
        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

    fun separateVersionJars() {
        separateVersionJars = true
        targets.forEach {
            TargetConfig.setUpJarTask(it)
        }
    }

    @JvmOverloads
    fun target(name: String, closure: Closure<*>? = null) {
        val ext = TargetExtension(this, project, name, closure == null)
        if (separateVersionJars) {
            TargetConfig.setUpJarTask(ext)
        }
        closure?.let { project.configure(ext, it) }
        targets.add(ext)
    }

    fun target(vararg versions: String) {
        targets += versions.map {
            val ext = TargetExtension(this, project, it, true)
            if (separateVersionJars) TargetConfig.setUpJarTask(ext)
            ext
        }
    }

    fun mainSourceSet(set: SourceSet) {
        mainSourceSet = set
    }

    @JvmOverloads
    fun mod(name: String, closure: Closure<*>? = null) {
        closure?.let { modContainer.create(name, it) } ?: modContainer.create(name)
    }

    fun mods(closure: Closure<*>) {
        modContainer.configure(closure)
    }
}