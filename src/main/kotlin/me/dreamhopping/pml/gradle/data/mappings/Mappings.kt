package me.dreamhopping.pml.gradle.data.mappings

import java.io.Serializable

data class Mappings(
    val classes: LinkedHashMap<String, String>,
    val methods: LinkedHashMap<String, String>,
    val fields: LinkedHashMap<String, String>,
    val locals: LinkedHashMap<String, String>
) : Serializable {
    fun className(obfuscated: String, name: String) {
        classes[obfuscated] = name
    }

    fun method(
        owner: String,
        obfuscated: String,
        desc: String,
        name: String
    ) {
        methods["$owner/$obfuscated$desc"] = name
    }

    fun field(owner: String, obfuscated: String, name: String) {
        fields["$owner/$obfuscated"] = name
    }

    companion object {
        inline fun mappings(callback: Mappings.() -> Unit) =
            Mappings(linkedMapOf(), linkedMapOf(), linkedMapOf(), linkedMapOf()).apply(callback)
    }
}