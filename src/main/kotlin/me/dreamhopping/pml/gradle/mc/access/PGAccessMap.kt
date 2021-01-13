package me.dreamhopping.pml.gradle.mc.access

import me.dreamhopping.pml.gradle.utils.Json
import net.md_5.specialsource.AccessMap
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
import java.io.BufferedReader

class PGAccessMap : AccessMap() {
    lateinit var maps: Remapper
    private val transformers = hashMapOf<String, AccessTransformer>()

    override fun loadAccessTransformer(reader: BufferedReader) {
        val extraTransformers = Json.parse<Map<String, AccessTransformer>>(reader.readText())
        synchronized(transformers) {
            for ((className, transformer) in extraTransformers) {
                // gson sets these to null if not present
                transformer.methods = transformer.methods ?: hashMapOf()
                transformer.fields = transformer.fields ?: hashMapOf()
                if (className in transformers) {
                    val current = transformers[className]!!
                    current.access = morePublicAccess(current.access, transformer.access)
                    for ((fieldName, access) in transformer.fields) {
                        current.fields[fieldName] = current.fields[fieldName]?.let { morePublicAccess(it, access) } ?: access
                    }
                    for ((methodId, access) in transformer.methods) {
                        current.methods[methodId] = current.methods[methodId]?.let { morePublicAccess(it, access) } ?: access
                    }
                } else {
                    transformers[className] = transformer
                }
            }
        }
    }

    private fun morePublicAccess(a: String, b: String): String {
        if (a == "public" || b == "public") return "public"
        if (a == "protected" || b == "protected") return "protected"
        if (a == "package" || b == "package") return "package"
        if (a == "private" || b == "private") return "private"
        return "keep" // impossible unless both 'keep'
    }

    override fun applyClassAccess(className: String, access: Int): Int {
        return transformers[maps.map(className)]?.access?.apply(access) ?: access
    }

    override fun applyFieldAccess(className: String, fieldName: String, access: Int): Int {
        val transformer = transformers[maps.map(className)] ?: return access
        return transformer.fields[maps.mapFieldName(className, fieldName, "")]?.apply(access) ?: access
    }

    override fun applyMethodAccess(className: String, methodName: String, methodDesc: String, access: Int): Int {
        val transformer = transformers[maps.map(className)] ?: return access
        val mappedDesc = maps.mapMethodDesc(methodDesc)
        val mappedName = maps.mapMethodName(className, methodName, methodDesc)
        return transformer.methods["$mappedName$mappedDesc"]?.apply(access) ?: access
    }

    private fun String.apply(access: Int) = when (this) {
        "public" -> (access and 0b10111.inv()) or Opcodes.ACC_PUBLIC
        "private" -> (access and 0b10111.inv()) or Opcodes.ACC_PRIVATE
        "protected" -> (access and 0b10111.inv()) or Opcodes.ACC_PROTECTED
        "package" -> access and 0b10111.inv()
        else -> access and Opcodes.ACC_FINAL.inv()
    }
}