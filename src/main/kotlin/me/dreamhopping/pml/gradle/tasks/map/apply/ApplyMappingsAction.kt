package me.dreamhopping.pml.gradle.tasks.map.apply

import me.dreamhopping.pml.gradle.tasks.map.apply.processors.ClassFixer
import me.dreamhopping.pml.gradle.tasks.map.apply.processors.InheritanceMap
import me.dreamhopping.pml.gradle.util.fromJson
import org.gradle.workers.WorkAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class ApplyMappingsAction : WorkAction<ApplyMappingsParameters> {
    override fun execute() {
        val input = parameters.inputJar.asFile.get()
        val mappings = parameters.mappings.asFile.get()
        val accessTransformers = parameters.accessTransformers.get()
        val output = parameters.outputJar.asFile.get()

        ZipFile(input).use { inputZip ->
            val inheritanceMap = InheritanceMap(inputZip)
            val mapper = MappingApplier(mappings.fromJson(), inheritanceMap)

            output.parentFile?.mkdirs()
            ZipOutputStream(output.outputStream()).use { outputZip ->
                for (entry in inputZip.entries()) {
                    if (entry.name.endsWith(".class")) {
                        val writer = ClassWriter(0)
                        val remapper = ClassRemapper(writer, mapper)
                        val classFixer = ClassFixer(remapper)

                        val reader = ClassReader(inputZip.getInputStream(entry).use { it.readBytes() })
                        reader.accept(inheritanceMap.createSaver(classFixer), 0)

                        outputZip.putNextEntry(ZipEntry("${mapper.map(entry.name.removeSuffix(".class"))}.class"))
                        outputZip.write(writer.toByteArray())
                        outputZip.closeEntry()
                    } else if (!entry.isDirectory) {
                        outputZip.putNextEntry(ZipEntry(entry.name))
                        inputZip.getInputStream(entry).use { it.copyTo(outputZip) }
                        outputZip.closeEntry()
                    }
                }
            }
        }
    }
}