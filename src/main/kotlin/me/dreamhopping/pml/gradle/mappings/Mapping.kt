package me.dreamhopping.pml.gradle.mappings

import java.io.Serializable

data class Mapping(val classes: MutableMap<String, ClassMap>, val locals: MutableMap<String, String>) : Serializable {
    constructor() : this(linkedMapOf(), linkedMapOf())

    fun ensureClassMapped(obfuscated: String, mapped: String) =
        classes.getOrPut(obfuscated) { ClassMap(mapped, hashMapOf(), hashMapOf()) }

    fun ensureParamMapped(obfuscated: String, mapped: String) = locals.putIfAbsent(obfuscated, mapped)

    fun clear() {
        classes.clear()
    }

    fun loadFrom(other: Mapping) {
        locals += other.locals
        for ((key, cl) in other.classes) {
            if (key !in classes) {
                classes[key] = cl
            } else {
                val classMap = classes[key]!!
                classMap.mappedName = cl.mappedName
                classMap.methods += cl.methods
                classMap.fields += cl.fields
            }
        }
    }

    data class ClassMap(
        var mappedName: String,
        val fields: MutableMap<String, String>,
        val methods: MutableMap<String, String>
    ) : Serializable {
        fun ensureFieldMapped(obfuscated: String, mapped: String) {
            fields.putIfAbsent(obfuscated, mapped)
        }

        fun ensureMethodMapped(obfuscated: String, obfuscatedDesc: String, mapped: String) =
            methods.putIfAbsent("$obfuscated/$obfuscatedDesc", mapped)
    }
}
