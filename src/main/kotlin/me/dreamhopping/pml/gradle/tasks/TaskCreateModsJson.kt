package me.dreamhopping.pml.gradle.tasks

import com.google.gson.JsonObject
import me.dreamhopping.pml.gradle.data.ModExt
import me.dreamhopping.pml.gradle.utils.Json
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class TaskCreateModsJson : DefaultTask() {
    @Input
    lateinit var mods: List<ModExt>
    @OutputFile
    lateinit var output: File

    @TaskAction
    fun create() {
        output.writeText(Json.GSON.toJson(mods))
    }
}