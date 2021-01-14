package me.dreamhopping.pml.gradle.tasks.mods

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.dreamhopping.pml.gradle.mods.ModExtension
import me.dreamhopping.pml.gradle.util.Json.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenModsJsonTask : DefaultTask() {
    @Internal
    lateinit var mods: Collection<ModExtension>

    @Input
    fun getModList() = mods.map { SerializedMod(it.name) }

    @OutputFile
    val outputFile = File(temporaryDir, "mods.json")

    @TaskAction
    fun generate() {
        val arr = JsonArray()
        getModList().forEach {
            val obj = JsonObject()

            obj.addProperty("id", it.name)
            obj.addProperty("version", project.version.toString())

            arr.add(obj)
        }
        arr.toJson(outputFile)
    }
}