package me.dreamhopping.pml.gradle.tasks

import kotlinx.coroutines.runBlocking
import me.dreamhopping.pml.gradle.mc.MinecraftSetup.cacheDir
import me.dreamhopping.pml.gradle.tasks.decompile.Decompiler
import me.dreamhopping.pml.gradle.utils.BetterURLClassLoader
import me.dreamhopping.pml.gradle.utils.http.Downloader
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskGenSources : DefaultTask() {
    @InputFile
    lateinit var inputJar: File

    @InputFiles
    lateinit var libraries: FileCollection

    @OutputFile
    lateinit var outputJar: File

    @TaskAction
    fun decompile() {
        // Run the actual decompiler with fernflower in the classpath
        val fernFlower = File(project.gradle.cacheDir, "libraries/forgeflower.jar")
        runBlocking { Downloader.download("http://files.minecraftforge.net/maven/net/minecraftforge/forgeflower/1.5.478.18/forgeflower-1.5.478.18.jar", fernFlower) }
        val classLoader = BetterURLClassLoader(arrayOf(fernFlower.toURI().toURL()), javaClass.classLoader)
        val decompilerClass = Class.forName(Decompiler::class.java.name, false, classLoader)
        val decompilerField = decompilerClass.getDeclaredField("INSTANCE")
        val decompilerMethod = decompilerClass.getDeclaredMethod("startDecompile", Project::class.java, File::class.java, File::class.java, FileCollection::class.java, File::class.java)
        decompilerMethod.invoke(decompilerField.get(null), project, inputJar, outputJar, libraries, fernFlower)
    }
}