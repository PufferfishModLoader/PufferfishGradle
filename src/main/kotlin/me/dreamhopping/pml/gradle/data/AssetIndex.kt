package me.dreamhopping.pml.gradle.data

data class AssetIndex(val objects: Map<String, Asset>) {
    data class Asset(val hash: String)
}
