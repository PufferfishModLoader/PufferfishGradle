package me.dreamhopping.pml.gradle.user

import groovy.lang.Closure
import me.dreamhopping.pml.gradle.target.TargetConfigurator
import me.dreamhopping.pml.gradle.util.java
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

@Suppress("unused", "MemberVisibilityCanBePrivate")
class UserData(val project: Project) {
    var mainSourceSet: SourceSet = project.java.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
    var separateVersionJars = false
        set(v) {
            if (!v) error("Turning off separation of version JARs is not supported")
            field = v
            targets.forEach {
                TargetConfigurator.setUpJarTasks(project, it)
            }
        }
    var loader = false
        set(v) {
            if (!v) error("Turning off loader is not supported")
            field = v
            targets.forEach {
                TargetConfigurator.setUpLoaderTasks(project, it)
            }
        }
    val targets = hashSetOf<TargetData>()

    fun separateVersionJars() {
        separateVersionJars = true
    }

    fun target(version: String, closure: Closure<*>? = null) {
        if (targets.any { it.version == version }) error("Version $version is already being targeted")
        targets.add(TargetData(project, version).also { data ->
            closure?.let { project.configure(data, it) }
            TargetConfigurator.configureTarget(project, data, this, closure == null)
        })
    }

    fun target(vararg versions: String) {
        versions.forEach { target(it) }
    }

    fun loader() {
        loader = true
    }
}