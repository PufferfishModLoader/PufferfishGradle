package me.dreamhopping.pml.gradle.data.mappings

data class Mappings(
    val classes: LinkedHashMap<String, String>,
    val methods: LinkedHashMap<String, MethodMaps>,
    val fields: LinkedHashMap<String, String>
) {
    fun className(obfuscated: String, name: String) {
        classes[obfuscated] = name
    }

    inline fun method(
        owner: String,
        obfuscated: String,
        desc: String,
        name: String,
        callback: MethodMaps.() -> Unit = {}
    ) {
        methods["$owner/$obfuscated$desc"] = MethodMaps(name, linkedMapOf()).apply(callback)
    }

    fun field(owner: String, obfuscated: String, name: String) {
        fields["$owner/$obfuscated"] = name
    }

    companion object {
        inline fun mappings(callback: Mappings.() -> Unit) =
            Mappings(linkedMapOf(), linkedMapOf(), linkedMapOf()).apply(callback)
    }
}