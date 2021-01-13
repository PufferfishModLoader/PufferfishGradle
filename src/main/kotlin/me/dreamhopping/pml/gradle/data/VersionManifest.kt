package me.dreamhopping.pml.gradle.data

import java.net.URL

data class VersionManifest(val versions: List<Entry>) {
    fun find(id: String) = versions.find { it.id == id }

    data class Entry(val id: String, val url: URL)
}
