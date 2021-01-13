package me.dreamhopping.pml.gradle.tasks.run

import org.gradle.workers.WorkAction
import java.io.File
import java.util.zip.ZipFile

abstract class ExtractNativesAction : WorkAction<ExtractNativesParameters> {
    override fun execute() {
        val files = parameters.files.get()
        val outputDir = parameters.outputDir.asFile.get()
        val pattern = parameters.pattern.get().toRegex()

        files.forEach { file ->
            ZipFile(file).use { zip ->
                for (entry in zip.entries()) {
                    if (!entry.isDirectory && pattern.matches(entry.name)) {
                        val f = File(outputDir, entry.name)
                        f.parentFile?.mkdirs()
                        f.outputStream().use { output -> zip.getInputStream(entry).use { it.copyTo(output) } }
                    }
                }
            }
        }
    }
}