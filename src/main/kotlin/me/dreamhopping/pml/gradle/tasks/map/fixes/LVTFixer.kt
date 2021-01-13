package me.dreamhopping.pml.gradle.tasks.map.fixes

import me.dreamhopping.pml.gradle.mappings.Mapping
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class LVTFixer(visitor: ClassVisitor?, private val map: Mapping, var currentIndex: Int) :
    ClassVisitor(Opcodes.ASM9, visitor) {
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return LVTMethodFixer(
            super.visitMethod(access, name, descriptor, signature, exceptions),
            map,
            this
        )
    }

    class LVTMethodFixer(
        visitor: MethodVisitor?,
        private val map: Mapping,
        private val owner: LVTFixer
    ) :
        MethodVisitor(Opcodes.ASM9, visitor) {

        override fun visitParameter(name: String, access: Int) {
            val idx = owner.currentIndex++
            val n = name.generateNameIfInvalid(idx)
            super.visitParameter(map.locals[n] ?: n, access)
        }

        override fun visitLocalVariable(
            name: String,
            descriptor: String?,
            signature: String?,
            start: Label?,
            end: Label?,
            index: Int
        ) {
            val idx = owner.currentIndex++
            val n = name.generateNameIfInvalid(idx)
            super.visitLocalVariable(
                map.locals[n] ?: n,
                descriptor,
                signature,
                start,
                end,
                index
            )
        }

        private fun String.generateNameIfInvalid(idx: Int) = takeIf { isValidIdentifier() } ?: "var$idx"

        private fun String.isValidIdentifier() =
            Character.isJavaIdentifierStart(this[0]) && all { Character.isJavaIdentifierPart(it) }
    }
}