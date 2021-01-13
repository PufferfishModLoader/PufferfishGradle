package me.dreamhopping.pml.gradle.tasks.strip

import org.gradle.workers.WorkAction
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class StripJarAction : WorkAction<StripJarParameters> {
    override fun execute() {
        val input = parameters.input.asFile.get()
        val classOutput = parameters.classOutput.asFile.get()
        val resourceOutput = parameters.resourceOutput.asFile.get()
        val allowedDirectories = parameters.allowedDirectories.get()

        classOutput.parentFile?.mkdirs()
        resourceOutput.parentFile?.mkdirs()
        ZipFile(input).use { inputJar ->
            ZipOutputStream(classOutput.outputStream()).use { outputJar ->
                ZipOutputStream(resourceOutput.outputStream()).use { resourceJar ->
                    for (entry in inputJar.entries()) {
                        if (entry.name.endsWith(".class")) {
                            if (!entry.name.contains('/') || allowedDirectories.any { entry.name.startsWith(it) }) {
                                val newEntry = ZipEntry(entry.name)
                                outputJar.putNextEntry(newEntry)
                                inputJar.getInputStream(entry).use { it.copyTo(outputJar) }
                                outputJar.closeEntry()
                            }
                        } else if (!entry.isDirectory) {
                            val newEntry = ZipEntry(entry.name)
                            resourceJar.putNextEntry(newEntry)
                            inputJar.getInputStream(entry).use { it.copyTo(resourceJar) }
                            resourceJar.closeEntry()
                        }
                    }
                }
            }
        }
    }
}