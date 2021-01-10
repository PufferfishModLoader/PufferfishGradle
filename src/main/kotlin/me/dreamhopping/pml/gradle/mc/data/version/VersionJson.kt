package me.dreamhopping.pml.gradle.mc.data.version

data class VersionJson(
    val assets: String,
    val assetIndex: AssetIndexDecl?,
    val downloads: RootDownloads,
    val id: String,
    val libraries: List<Library>
)