package me.dreamhopping.pml.gradle.tasks.decompile

import org.gradle.workers.WorkAction
import org.jetbrains.java.decompiler.main.Fernflower
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences.*
import java.io.File
import java.io.PrintStream

abstract class DecompileAction : WorkAction<DecompileParameters> {
    override fun execute() {
        val input = parameters.input.asFile.get()
        val libraries = parameters.libraries.get()
        val mappings = parameters.mappings.asFile.get()
        val output = parameters.output.asFile.get()

        // TODO: Implement local variable renaming

        val options = mapOf(
            HIDE_DEFAULT_CONSTRUCTOR to "0",
            DECOMPILE_GENERIC_SIGNATURES to "1",
            REMOVE_SYNTHETIC to "1",
            REMOVE_BRIDGE to "1",
            // LITERALS_AS_IS to "1",
            NEW_LINE_SEPARATOR to "1",
            IGNORE_INVALID_BYTECODE to "1",
            VERIFY_ANONYMOUS_CLASSES to "1",
            BYTECODE_SOURCE_MAPPING to "1",
            USE_DEBUG_VAR_NAMES to "1",
            LOG_LEVEL to IFernflowerLogger.Severity.ERROR.name,
            THREADS to Runtime.getRuntime().availableProcessors().toString()
            // RENAMER_FACTORY to VariableNameProvider.Factory::class.java.name
        )

        /*VariableNameProvider.mappings.clear()
        VariableNameProvider.mappings.loadFrom(Mapping(mappings.fromJson()))*/

        val saver = ResultSaver(output)
        val logger = PrintStreamLogger(PrintStream(System.err))
        val decompiler = Fernflower(BytecodeProvider, saver, options, logger)

        decompiler.addSource(input)

        for (library in libraries) {
            if (library.exists()) {
                decompiler.addLibrary(library)
            }
        }

        System.getProperty("sun.boot.class.path")?.split(File.pathSeparator)?.forEach {
            val file = File(it)
            if (file.exists()) {
                decompiler.addLibrary(file)
            }
        } ?: println("Running on Java 9 or higher, decompiler output might not be accurate.")

        decompiler.decompileContext()

        System.gc()
    }
}