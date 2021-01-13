package me.dreamhopping.pml.gradle.tasks.decompile

import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider
import org.jetbrains.java.decompiler.util.InterpreterUtil
import java.io.File
import java.util.zip.ZipFile

object BytecodeProvider : IBytecodeProvider {
    override fun getBytecode(externalPath: String, internalPath: String?): ByteArray {
        val file = File(externalPath)
        return internalPath?.let {
            ZipFile(file).use { zip ->
                InterpreterUtil.getBytes(
                    zip,
                    zip.getEntry(it) ?: error("entry not found: $internalPath")
                )
            }
        } ?: InterpreterUtil.getBytes(file)
    }
}