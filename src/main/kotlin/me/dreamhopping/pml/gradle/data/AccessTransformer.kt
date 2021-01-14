package me.dreamhopping.pml.gradle.data

data class AccessTransformer(
    var access: String?,
    var fields: MutableMap<String, String>?,
    var methods: MutableMap<String, String>?
)