package me.dreamhopping.pml.gradle

import groovy.lang.Closure
import me.dreamhopping.pml.gradle.targets.TargetExtension
import org.gradle.api.Project

class PGExtension(val project: Project) {
    val targets = arrayListOf<TargetExtension>()

    @JvmOverloads
    fun target(name: String, closure: Closure<*>? = null) {
        val ext = TargetExtension(project, name, closure == null)
        closure?.let { project.configure(ext, it) }
        targets.add(ext)
    }
}