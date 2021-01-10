package me.dreamhopping.pml.gradle.tasks.decompile

import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec

object ForkUtil {
    fun exec(project: Project, action: JavaExecSpec.() -> Unit): ExecResult {
        val configurations = project.buildscript.configurations
        val handler = project.dependencies
        val classpath = configurations.getByName("classpath")
            .plus(configurations.detachedConfiguration(handler.localGroovy()))
        return project.javaexec {
            it.classpath(classpath)
            it.action()
        }
    }
}