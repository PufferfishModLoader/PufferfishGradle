package me.dreamhopping.pml.gradle.tasks.strip

import org.gradle.workers.WorkAction
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class StripAction : WorkAction<StripParameters> {
    override fun execute() {
        val input = parameters.input.asFile.get()
        val classOutput = parameters.classOutput.asFile.get()
        val resourceOutput = parameters.resourceOutput.asFile.get()
        val allowedDirectories = parameters.allowedDirectories.get()

        ZipFile(input).use { inputZip ->
            classOutput.parentFile?.mkdirs()
            ZipOutputStream(classOutput.outputStream()).use { classOutputZip ->
                ZipOutputStream(resourceOutput.outputStream()).use { resourceOutputZip ->
                    for (entry in inputZip.entries()) {
                        if (entry.name.endsWith(".class")) {
                            if (!entry.name.contains('/') || allowedDirectories.any { entry.name.startsWith(it) }) {
                                inputZip.copyEntry(entry, classOutputZip)
                            }
                        } else if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                            inputZip.copyEntry(entry, resourceOutputZip)
                        }
                    }
                }
            }
        }
    }

    private fun ZipFile.copyEntry(entry: ZipEntry, to: ZipOutputStream) {
        to.putNextEntry(ZipEntry(entry.name))
        getInputStream(entry).use { it.copyTo(to) }
        to.closeEntry()
    }
}