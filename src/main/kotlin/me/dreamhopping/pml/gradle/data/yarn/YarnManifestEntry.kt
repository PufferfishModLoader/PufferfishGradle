package me.dreamhopping.pml.gradle.data.yarn

data class YarnManifestEntry(val mappings: YarnArtifactData) {
    data class YarnArtifactData(val version: String)
}