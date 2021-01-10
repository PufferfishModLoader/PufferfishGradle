package me.dreamhopping.pml.gradle.mc

import java.io.File
import java.nio.file.attribute.FileTime
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

object JarStripper {
    private val ALLOWED_DIRECTORIES = arrayOf(
        "assets",
        "net/minecraft"
    )

    fun strip(input: File, output: File) {
        output.parentFile?.mkdirs()
        JarFile(input).use { inputJar ->
            JarOutputStream(output.outputStream()).use { outputJar ->
                inputJar.entries().iterator().forEach { entry ->
                    if (isAllowed(entry.name)) {
                        outputJar.putNextEntry(ZipEntry(entry.name).also {
                            it.lastModifiedTime = FileTime.fromMillis(System.currentTimeMillis())
                        })
                        inputJar.getInputStream(entry).use { it.copyTo(outputJar) }
                        outputJar.closeEntry()
                    }
                }
            }
        }
    }

    private fun isAllowed(name: String): Boolean {
        if (name.contains('/')) {
            ALLOWED_DIRECTORIES.forEach {
                if (name.startsWith(it)) return true
            }
            return false
        }
        return true
    }
}