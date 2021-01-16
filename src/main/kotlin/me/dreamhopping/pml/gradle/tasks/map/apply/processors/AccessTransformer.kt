package me.dreamhopping.pml.gradle.tasks.map.apply.processors

import me.dreamhopping.pml.gradle.util.fromJson
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.*

class AccessTransformer(private val data: AccessTransformerData, visitor: ClassVisitor) :
    ClassVisitor(Opcodes.ASM9, visitor) {
    private var className: String? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        super.visit(version, data.classes[name].apply(access), name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return super.visitMethod(data.methods["$className/$name$descriptor"].apply(access), name, descriptor, signature, exceptions)
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        return super.visitField(data.fields["$className/$name"].apply(access), name, descriptor, signature, value)
    }

    private fun AccessTransformerData.TransformerAction?.apply(current: Int): Int {
        if (this == null) return current
        val cur = when {
            current.has(Opcodes.ACC_PRIVATE) -> Access.PRIVATE
            current.has(Opcodes.ACC_PROTECTED) -> Access.PROTECTED
            current.has(Opcodes.ACC_PUBLIC) -> Access.PUBLIC
            else -> Access.PUBLIC
        }
        var c = current and 0b111.inv() or cur.opcode // package private -> public
        if (access >= cur) {
            c = c and 0b111.inv() or access.opcode
        }
        if (removeFinal) {
            c = c and Opcodes.ACC_FINAL.inv()
        }
        return c
    }

    private fun Int.has(b: Int) = this and b != 0

    companion object {
        fun parse(transformers: Set<File>): AccessTransformerData {
            val classes = linkedMapOf<String, AccessTransformerData.TransformerAction>()
            val methods = linkedMapOf<String, AccessTransformerData.TransformerAction>()
            val fields = linkedMapOf<String, AccessTransformerData.TransformerAction>()

            transformers.forEach { transformer ->
                val data = transformer.fromJson<Map<String, OnDiskATData>>()
                data.forEach { (className, action) ->
                    action.access?.toAction()?.let { classes[className] = classes[className].merge(it) }
                    action.methods?.map { it.key to it.value.toAction() }?.forEach {
                        methods["$className/${it.first}"] = methods["$className/${it.first}"].merge(it.second)
                    }
                    action.fields?.map { it.key to it.value.toAction() }?.forEach {
                        fields["$className/${it.first}"] = fields["$className/${it.first}"].merge(it.second)
                    }
                }
            }

            return AccessTransformerData(classes, methods, fields)
        }

        private fun AccessTransformerData.TransformerAction?.merge(other: AccessTransformerData.TransformerAction) =
            this?.let {
                AccessTransformerData.TransformerAction(
                    maxOf(access, other.access), removeFinal || other.removeFinal
                )
            } ?: other

        private fun String.toAction(): AccessTransformerData.TransformerAction {
            val lastIndex =
                lastIndexOf('-').takeUnless { it == -1 } ?: length
            val str = substring(0, lastIndex)
            return AccessTransformerData.TransformerAction(str.takeIf { it.isNotEmpty() && !it.equals("protected", true) }
                ?.let { Access.valueOf(it.toUpperCase(Locale.ENGLISH)) } ?: Access.values()[0], lastIndex != length)
        }
    }

    private data class OnDiskATData(val access: String?, val fields: Map<String, String>?, val methods: Map<String, String>?)

    data class AccessTransformerData(
        val classes: Map<String, TransformerAction>,
        val methods: Map<String, TransformerAction>,
        val fields: Map<String, TransformerAction>
    ) {
        data class TransformerAction(var access: Access, var removeFinal: Boolean)
    }

    enum class Access(val opcode: Int) {
        PRIVATE(Opcodes.ACC_PRIVATE),
        PROTECTED(Opcodes.ACC_PROTECTED),
        PUBLIC(Opcodes.ACC_PUBLIC),
    }
}