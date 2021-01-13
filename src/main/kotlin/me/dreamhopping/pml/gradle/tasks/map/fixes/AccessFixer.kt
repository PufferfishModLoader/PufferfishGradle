package me.dreamhopping.pml.gradle.tasks.map.fixes

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AccessFixer(classVisitor: ClassVisitor?) : ClassVisitor(Opcodes.ASM9, classVisitor) {
    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access.fixAccess(), name, signature, superName, interfaces)
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        return super.visitField(access.fixAccess(), name, descriptor, signature, value)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return super.visitMethod(access.fixAccess(), name, descriptor, signature, exceptions)
    }

    private fun Int.fixAccess() = (this and 0b111.inv() or Opcodes.ACC_PUBLIC).takeUnless { (this and Opcodes.ACC_PRIVATE) != 0 } ?: this
}