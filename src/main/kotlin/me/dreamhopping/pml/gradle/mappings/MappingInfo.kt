package me.dreamhopping.pml.gradle.mappings

import java.io.Serializable

data class MappingInfo(
    val classes: Map<String, String>,
    val fields: Map<String, String>,
    val methods: Map<Pair<String, String>, String>,
    val packages: Map<String, String>
) : Serializable
