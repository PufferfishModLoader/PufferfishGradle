package me.dreamhopping.pml.gradle.data.minecraft

import me.dreamhopping.pml.gradle.util.currentMcOs

data class Library(val name: String, val natives: Map<String, String>?, val rules: List<Rule>?) {
    fun getId() = "$name${getNative()?.let { ":$it" } ?: ""}"

    fun getNative() = natives?.get(currentMcOs())?.replace(
        "\${arch}",
        if (System.getProperty("os.arch") in X64_ARCHITECTURES) "64" else "32"
    )

    fun allowed() = rules == null || rules.isEmpty() || rules.lastOrNull { it.appliesToCurrent() }?.action == "allow"

    companion object {
        private val X64_ARCHITECTURES = arrayOf("amd64")
    }
}