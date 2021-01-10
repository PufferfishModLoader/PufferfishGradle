package me.dreamhopping.pml.gradle.tasks

import me.dreamhopping.pml.runtime.Start
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File
import java.io.InputStream
import java.io.PrintStream

open class TaskRunGame : DefaultTask() {
    @Input
    lateinit var runDir: String

    @Input
    lateinit var mainClass: String

    @Input
    lateinit var assetDirectory: String

    @Input
    lateinit var arguments: MutableList<String>

    @Input
    lateinit var vmArguments: MutableList<String>

    @Input
    lateinit var assetIndex: String

    @Input
    var server = false

    @Classpath
    @InputFiles
    lateinit var classpath: FileCollection

    @TaskAction
    fun run() {
        File(runDir).mkdirs()
        val builder = ProcessBuilder()
            .command(
                "${System.getProperty("java.home")}/bin/java",
                "-cp",
                classpath.joinToString(File.pathSeparator) { it.absolutePath },
                Start::class.java.name,
                *arguments.toTypedArray()
            )
            .directory(File(runDir).absoluteFile)
        builder.environment().also {
            it["PML_MAIN_CLASS"] = mainClass
            it["PML_ASSET_DIRECTORY"] = File(assetDirectory).absolutePath
            it["PML_ASSET_INDEX"] = assetIndex
            it["PML_RUN_DIRECTORY"] = File(runDir).absolutePath
            it["PML_IS_SERVER"] = server.toString()
        }
        val proc = builder.start()
        val out = RedirectOutputThread(proc.inputStream, System.out)
        val err = RedirectOutputThread(proc.errorStream, System.err)
        out.start()
        err.start()
        proc.waitFor()
        out.join()
        err.join()
    }

    private class RedirectOutputThread(val input: InputStream, val output: PrintStream) : Thread() {
        override fun run() {
            val buf = ByteArray(4096)
            while (true) {
                val i = input.read(buf)
                if (i < 0) break
                output.write(buf, 0, i)
            }
        }
    }
}