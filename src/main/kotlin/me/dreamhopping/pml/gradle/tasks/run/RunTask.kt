package me.dreamhopping.pml.gradle.tasks.run

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class RunTask : DefaultTask(), IRunTask {
    @Classpath
    override var classpath: FileCollection? = null

    @Input
    override var mainClass: String? = null

    @Input
    override var args: List<String>? = null

    @Input
    override var vmArgs: List<String>? = null

    @Input
    override var environment: Map<String, String>? = null

    @Input
    override var runDir: String? = null

    @TaskAction
    fun run() {
        project.javaexec {
            it.main = mainClass
            it.classpath = classpath
            it.args = args
            it.jvmArgs = vmArgs
            it.environment = environment
            it.workingDir = project.file(runDir!!)
        }
    }
}