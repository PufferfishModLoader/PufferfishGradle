package me.dreamhopping.pml.gradle.mc.data.version

data class AssetIndexDecl(
    val id: String?,
    override val sha1: String?,
    override val size: Long?,
    override val url: String?
) : IArtifact
