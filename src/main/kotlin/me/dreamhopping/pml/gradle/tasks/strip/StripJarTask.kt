package me.dreamhopping.pml.gradle.tasks.strip

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class StripJarTask : DefaultTask() {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    lateinit var input: File

    @OutputFile
    lateinit var classOutput: File

    @OutputFile
    lateinit var resourceOutput: File

    @Input
    val allowedDirectories = arrayListOf<String>()

    // kt doesnt add annotations to getters; we need this to be a method
    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    @TaskAction
    fun stripJar() {
        val queue = getWorkerExecutor().noIsolation()

        queue.submit(StripJarAction::class.java) {
            it.input.set(input)
            it.classOutput.set(classOutput)
            it.resourceOutput.set(resourceOutput)
            it.allowedDirectories.set(allowedDirectories)
        }

        /*classOutput.parentFile?.mkdirs()
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
                        } else {
                            val newEntry = ZipEntry(entry.name)
                            resourceJar.putNextEntry(newEntry)
                            inputJar.getInputStream(entry).use { it.copyTo(resourceJar) }
                            resourceJar.closeEntry()
                        }
                    }
                }
            }
        }*/
    }
}