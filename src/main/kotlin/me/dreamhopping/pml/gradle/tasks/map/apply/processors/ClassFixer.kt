package me.dreamhopping.pml.gradle.tasks.map.apply.processors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ClassFixer(visitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, visitor) {
    private var className: String? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitSource(source: String?, debug: String?) {
        super.visitSource(className?.getSourceFileFromName() ?: "SourceFile", debug)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return MethodDebugStripper(super.visitMethod(access, name, descriptor, signature, exceptions))
    }

    private fun String.getSourceFileFromName() = "${substring(lastIndexOf('/') + 1).stripSubclasses()}.java"

    private fun String.stripSubclasses() = indexOf('$').takeIf { it >= 1 }?.let { substring(0, it) } ?: this

    private class MethodDebugStripper(visitor: MethodVisitor) : MethodVisitor(Opcodes.ASM9, visitor) {
        override fun visitLocalVariable(
            name: String?,
            descriptor: String?,
            signature: String?,
            start: Label?,
            end: Label?,
            index: Int
        ) {

        }

        override fun visitParameter(name: String?, access: Int) {

        }
    }
}