package me.dreamhopping.pml.gradle.mc.access

data class AccessTransformer(var access: String, var fields: MutableMap<String, String>, var methods: MutableMap<String, String>)