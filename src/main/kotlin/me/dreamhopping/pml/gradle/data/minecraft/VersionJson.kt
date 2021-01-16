package me.dreamhopping.pml.gradle.data.minecraft

data class VersionJson(
    val id: String,
    val downloads: McDownloads,
    val libraries: List<Library>,
    val assets: String,
    val assetIndex: UrlRequiredArtifact
)
