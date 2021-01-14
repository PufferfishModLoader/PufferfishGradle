package me.dreamhopping.pml.gradle.tasks.map.access

import me.dreamhopping.pml.gradle.data.AccessTransformer
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AccessMapper(classVisitor: ClassVisitor?, private val map: Map<String, List<AccessTransformer>>) :
    ClassVisitor(Opcodes.ASM9, classVisitor) {
    private lateinit var className: String

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        val transformer = map[name]
        if (transformer != null) {
            var ac = access
            ac = transformer.maxBy { it.access?.toIntLevel() ?: 0 }?.access?.fixAccess(ac) ?: ac
            super.visit(version, ac, name, signature, superName, interfaces)
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        val transformer = map[className]
        if (transformer != null) {
            val ac =
                transformer
                    .filter { it.fields?.containsKey(name) ?: false }
                    .maxBy { it.fields?.get(name)?.toIntLevel() ?: 0 }
                    ?.fields?.get(name) ?: "keep"
            return super.visitField(ac.fixAccess(access), name, descriptor, signature, value)
        }
        return super.visitField(access, name, descriptor, signature, value)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val transformer = map[className]
        if (transformer != null) {
            val key = "$name$descriptor"
            val ac =
                transformer
                    .filter { it.methods?.containsKey(key) ?: false }
                    .maxBy { it.methods?.get(key)?.toIntLevel() ?: 0 }
                    ?.methods?.get(key) ?: "keep"
            return super.visitMethod(ac.fixAccess(access), name, descriptor, signature, exceptions)
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    private fun String.toIntLevel() = when (this) {
        "public" -> 4
        "protected" -> 3
        "package" -> 2
        "private" -> 1
        else -> 0
    }

    private fun String.fixAccess(ac: Int) = when (toIntLevel()) {
        4 -> ac.withoutAccess or Opcodes.ACC_PUBLIC
        3 -> ac.withoutAccess or Opcodes.ACC_PROTECTED
        2 -> ac.withoutAccess
        1 -> ac.withoutAccess or Opcodes.ACC_PRIVATE
        else -> ac
    }

    private val Int.withoutAccess get() = this and 0b10111.inv()
}