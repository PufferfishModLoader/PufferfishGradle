package me.dreamhopping.pml.gradle.mc.deobf

import net.md_5.specialsource.RemapperProcessor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

object DecompilationPreparer : RemapperProcessor(null, null, null) {
    override fun process(classReader: ClassReader): ByteArray {
        val node = ClassNode()
        classReader.accept(node, 0)

        val actualName = node.name.substring(node.name.lastIndexOf('/') + 1)
        node.sourceFile = "$actualName.java"

        node.methods.forEach { method ->
            var currentIdx = 0
            method.parameters?.forEach {
                if (method.name == "<init>") println("param ${it.name} valid ${it.name.isValidIdentifier}")
                if (!it.name.isValidIdentifier) it.name = "var${currentIdx++}"
            }
            method.localVariables?.forEach {
                if (method.name == "<init>") println("param ${it.name} valid ${it.name.isValidIdentifier}")
                if (!it.name.isValidIdentifier) it.name = "var${currentIdx++}"
            }
        }

        val writer = ClassWriter(0)
        node.accept(writer)
        return writer.toByteArray()
    }

    private val String.isValidIdentifier get() = Character.isJavaIdentifierStart(this[0]) && !any { !Character.isJavaIdentifierPart(it) }
}