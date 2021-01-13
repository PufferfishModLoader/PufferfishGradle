package me.dreamhopping.pml.gradle.data.version

data class VersionJson(
    val id: String,
    val downloads: Map<String, Artifact>,
    val libraries: List<Library>,
    val assetIndex: Artifact,
    val assets: String
)