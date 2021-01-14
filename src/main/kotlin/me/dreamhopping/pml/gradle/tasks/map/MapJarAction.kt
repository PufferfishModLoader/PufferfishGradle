package me.dreamhopping.pml.gradle.tasks.map

import me.dreamhopping.pml.gradle.data.AccessTransformer
import me.dreamhopping.pml.gradle.mappings.Mapping
import me.dreamhopping.pml.gradle.tasks.map.access.AccessMapper
import me.dreamhopping.pml.gradle.tasks.map.fixes.AccessFixer
import me.dreamhopping.pml.gradle.tasks.map.fixes.LVTFixer
import me.dreamhopping.pml.gradle.tasks.map.fixes.SourceFileFixer
import me.dreamhopping.pml.gradle.util.Json.fromJson
import org.gradle.workers.WorkAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class MapJarAction : WorkAction<MapJarParameters> {
    override fun execute() {
        val inputJar = parameters.input.asFile.get()
        val mappingsFile = parameters.mappings.asFile.get()
        val accessMap = loadAccessMap(parameters.accessTransformers.get())
        val outputJar = parameters.output.asFile.get()

        val mappings = mappingsFile.fromJson<Mapping>()

        outputJar.parentFile?.mkdirs()

        ZipFile(inputJar).use { input ->
            ZipOutputStream(outputJar.outputStream()).use { output ->
                val map: InheritanceMap = hashMapOf()
                var currentIndex = 0
                for (entry in input.entries().toList().sortedBy { it.name }) {
                    if (entry.name.endsWith(".class")) {
                        val writer = ClassWriter(0)
                        val reader = ClassReader(input.getInputStream(entry).use { it.readBytes() })
                        val saver = MapJarNewClassNameSaver(writer)
                        val fixer = LVTFixer(
                            ClassRemapper(SourceFileFixer(AccessMapper(saver, accessMap)), PGRemapper(mappings, map) { name ->
                                input.getEntry("$name.class")?.let { entry1 ->
                                    val reader1 = ClassReader(input.getInputStream(entry1).use { it.readBytes() })
                                    val node = ClassNode()
                                    reader1.accept(node, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
                                    setOf(*(node.interfaces?.toTypedArray() ?: emptyArray()), node.superName ?: null).filterNotNull().toSet()
                                } ?: emptySet()
                            }),
                            mappings,
                            currentIndex
                        )
                        val visitor = AccessFixer(fixer)

                        reader.accept(visitor, 0)

                        currentIndex = fixer.currentIndex

                        output.putNextEntry(ZipEntry("${saver.className}.class"))
                        output.write(writer.toByteArray())
                        output.closeEntry()
                    } else {
                        output.putNextEntry(ZipEntry(entry.name))
                        input.getInputStream(entry).use { it.copyTo(output) }
                        output.closeEntry()
                    }
                }
            }
        }
    }

    private fun loadAccessMap(files: List<File>) = hashMapOf<String, MutableList<AccessTransformer>>().apply {
        files.forEach { file ->
            val map = file.fromJson<Map<String, AccessTransformer>>()
            map.forEach {
                getOrPut(it.key) { arrayListOf() }.add(it.value)
            }
        }
    }

    private class MapJarNewClassNameSaver(visitor: ClassVisitor?) : ClassVisitor(Opcodes.ASM9, visitor) {
        lateinit var className: String

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
    }
}