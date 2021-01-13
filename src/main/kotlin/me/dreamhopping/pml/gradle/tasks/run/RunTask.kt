package me.dreamhopping.pml.gradle.tasks.run

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RunTask : DefaultTask() {
    @Input
    lateinit var vmArgs: List<String>

    @Input
    lateinit var args: List<String>

    @Input
    lateinit var workDir: String

    @Input
    lateinit var environment: Map<String, () -> String>

    @Input
    lateinit var mainClass: String

    @Classpath
    lateinit var classpath: FileCollection

    @TaskAction
    fun run() {
        project.javaexec {
            it.classpath = classpath
            it.jvmArgs = vmArgs
            it.args = args
            it.workingDir = File(workDir)
            it.environment = mapOf(*environment.map { (key, value) -> key to value() }.toTypedArray())
            it.main = mainClass
        }
    }
}