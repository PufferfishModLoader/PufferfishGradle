package me.dreamhopping.pml.gradle.mappings

import groovy.lang.Closure
import me.dreamhopping.pml.gradle.util.Hash.toHexString
import me.dreamhopping.pml.gradle.util.Json.fromJson
import me.dreamhopping.pml.gradle.util.Json.toJson
import me.dreamhopping.pml.gradle.util.LOAD_MAPPINGS_BASE_NAME
import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.security.MessageDigest

class CustomMappingProvider(val project: Project) : MappingProvider {
    override val id get() = "custom-${createSha1(mappings)}"
    private val mappings = Mapping()

    fun from(file: Any) {
        mappings.loadFrom(project.file(file).fromJson())
    }

    @JvmOverloads
    fun className(obfuscated: String, mapped: String, closure: Closure<*>? = null) {
        val map = Mapping.ClassMap(mapped, linkedMapOf(), linkedMapOf())
        if (closure != null) {
            project.configure(ClassConfigure(project, map), closure)
        }
        mappings.classes[obfuscated] = map
    }

    fun local(obfuscated: String, mapped: String) {
        mappings.locals[obfuscated] = mapped
    }

    private fun createSha1(mapping: Mapping) = MessageDigest.getInstance("SHA-1").run {
        ByteArrayOutputStream().use {
            ObjectOutputStream(it).writeObject(mapping)
            update(it.toByteArray())
        }

        digest().toHexString()
    }

    override fun fetchIdFromDiskIfPossible(project: Project, minecraftVersion: String) {

    }

    override fun setUpVersionSetupTasks(
        project: Project,
        id: String,
        minecraftVersion: () -> String
    ): Pair<String, () -> File> {
        val out = File(project.buildDir, "tmp/$LOAD_MAPPINGS_BASE_NAME$id/output.json")
        val task = project.tasks.register("$LOAD_MAPPINGS_BASE_NAME$id") {
            it.inputs.property("mappings", mappings)
            it.outputs.file(out)
            it.doFirst {
                out.parentFile?.mkdirs()
                mappings.toJson(out)
            }
        }
        return task.name to { out }
    }

    private class ClassConfigure(val project: Project, val map: Mapping.ClassMap) {
        fun field(obfuscated: String, mapped: String) {
            map.fields[obfuscated] = mapped
        }

        fun method(obfuscated: String, descriptor: String, mapped: String) {
            map.methods["$obfuscated/$descriptor"] = mapped
        }
    }
}