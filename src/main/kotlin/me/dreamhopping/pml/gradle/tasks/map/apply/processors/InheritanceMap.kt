package me.dreamhopping.pml.gradle.tasks.map.apply.processors

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.util.zip.ZipFile

class InheritanceMap(private val zip: ZipFile) : HashMap<String, List<String>>() {
    fun createSaver(visitor: ClassVisitor?) = InheritanceSaver(visitor, this)

    fun getInheritance(name: String) = getOrPut(name) {
        val entry = zip.getEntry("$name.class") ?: return@getOrPut emptyList()
        val reader = ClassReader(zip.getInputStream(entry).use { it.readBytes() })
        reader.accept(createSaver(null), ClassReader.SKIP_FRAMES or ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG)
        get(name) ?: emptyList()
    }

    class InheritanceSaver(visitor: ClassVisitor?, private val map: MutableMap<String, List<String>>) :
        ClassVisitor(Opcodes.ASM9, visitor) {
        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>
        ) {
            map[name] = listOfNotNull(*interfaces, superName)
            super.visit(version, access, name, signature, superName, interfaces)
        }
    }
}