package me.dreamhopping.pml.gradle.data.mappings

data class MethodMaps(var mappedName: String, val locals: LinkedHashMap<String, String>) {
    fun local(obfuscated: String, name: String) {
        locals[obfuscated] = name
    }
}