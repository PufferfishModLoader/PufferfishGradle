package me.dreamhopping.pml.gradle.tasks.map.fixes

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

class SourceFileFixer(visitor: ClassVisitor?) : ClassVisitor(Opcodes.ASM9, visitor) {
    private lateinit var className: String
    private var hasSourceFile = false

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitSource(source: String?, debug: String?) {
        hasSourceFile = true
        super.visitSource(className.substring(className.lastIndexOf('/') + 1).removeSubclasses() + ".java", debug)
    }

    private fun String.removeSubclasses() = indexOf('$').takeIf { it != -1 }?.let { substring(0, it) } ?: this

    override fun visitEnd() {
        if (!hasSourceFile) visitSource(null, null)
        super.visitEnd()
    }
}