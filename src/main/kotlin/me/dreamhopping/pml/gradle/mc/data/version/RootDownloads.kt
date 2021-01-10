package me.dreamhopping.pml.gradle.mc.data.version

import org.gradle.internal.impldep.com.google.gson.annotations.SerializedName

data class RootDownloads(
    val client: Artifact,
    val server: Artifact,
    @SerializedName("windows_server") val windowsServer: Artifact?
)
