package me.dreamhopping.pml.gradle.tasks.loader

import me.dreamhopping.pml.gradle.util.sha1
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CreateClassPathInfoTask : DefaultTask() {
    @Classpath
    var classpath: Configuration? = null

    @OutputFile
    var output: File? = null

    @TaskAction
    fun create() {
        output?.bufferedWriter()?.use { writer ->
            classpath?.forEach {
                writer.write(it.sha1())
                writer.newLine()
            }
        }
    }
}