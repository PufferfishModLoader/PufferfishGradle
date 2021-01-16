package me.dreamhopping.pml.gradle.data.minecraft

import com.google.gson.annotations.SerializedName

data class AssetIndex(
    val objects: Map<String, Asset>,
    val virtual: Boolean?,
    @SerializedName("map_to_resources") val mapToResources: Boolean?
)
