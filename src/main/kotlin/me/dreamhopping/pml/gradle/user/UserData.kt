package me.dreamhopping.pml.gradle.user

import groovy.lang.Closure
import me.dreamhopping.pml.gradle.target.TargetConfigurator
import org.gradle.api.Project

@Suppress("unused", "MemberVisibilityCanBePrivate")
class UserData(val project: Project) {
    var separateVersionJars = true
        set(v) {
            if (!v) error("Turning off separation of version JARs is not supported")
            field = v
            targets.forEach {
                TargetConfigurator.setUpJarTasks(project, it)
            }
        }
    val targets = hashSetOf<TargetData>()

    fun separateVersionJars() {
        separateVersionJars = true
    }

    fun target(version: String, closure: Closure<*>? = null) {
        targets.add(TargetData(project, version).also { data ->
            closure?.let { project.configure(data, it) }
            TargetConfigurator.configureTarget(project, data, this, closure == null)
        })
    }

    fun target(vararg versions: String) {
        versions.forEach { target(it) }
    }
}