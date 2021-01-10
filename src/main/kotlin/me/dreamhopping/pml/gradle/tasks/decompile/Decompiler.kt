package me.dreamhopping.pml.gradle.tasks.decompile

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences.*
import java.io.File

object Decompiler {
    @Suppress("UNUSED")
    fun startDecompile(project: Project, input: File, output: File, libraries: FileCollection, decompiler: File) {
        decompile(project, input, output, libraries, decompiler)
    }

    private fun decompile(project: Project, input: File, output: File, libraries: FileCollection, decompiler: File) {
        project.logging.captureStandardOutput(LogLevel.LIFECYCLE)

        val options = mapOf(
            DECOMPILE_GENERIC_SIGNATURES to "1",
            BYTECODE_SOURCE_MAPPING to "1",
            REMOVE_SYNTHETIC to "1",
            LOG_LEVEL to "warning",
            THREADS to Runtime.getRuntime().availableProcessors().toString()
        )

        val result = ForkUtil.exec(project) {
            main = DecompilerMain::class.java.name
            classpath(decompiler)
            options.forEach {
                systemProperty("pg.fernflower.${it.key}", it.value)
            }
            systemProperty("pg.output", output.absolutePath)
            systemProperty("pg.input", input.absolutePath)
            args(libraries.map { it.absolutePath })
            jvmArgs("-Xms200M", "-Xmx3G")
            errorOutput = System.err
            standardOutput = System.out
        }

        result.rethrowFailure()
        result.assertNormalExitValue()
    }
}