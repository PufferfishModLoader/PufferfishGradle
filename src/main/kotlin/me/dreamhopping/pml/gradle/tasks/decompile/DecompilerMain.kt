package me.dreamhopping.pml.gradle.tasks.decompile

import org.jetbrains.java.decompiler.main.Fernflower
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

object DecompilerMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val options = hashMapOf<String, Any>()

        System.getProperties().forEach { t, u ->
            if (t.toString().startsWith("pg.fernflower.")) {
                options[t.toString().substring("pg.fernflower.".length)] = u
            }
        }

        val libraries = args.map { File(it) }
        val input = File(System.getProperty("pg.input"))
        val output = File(System.getProperty("pg.output"))

        // options[PROPERTY_NAME] = FernflowerJavadocProvider()
        val saver = FernflowerResultSaver(output)
        val logger = PrintStreamLogger(PrintStream(ByteArrayOutputStream()))
        val decompiler = Fernflower(BytecodeProvider, saver, options, logger)
        for (f in libraries) {
            decompiler.addLibrary(f)
        }
        decompiler.addSource(input)
        decompiler.decompileContext()
    }
}