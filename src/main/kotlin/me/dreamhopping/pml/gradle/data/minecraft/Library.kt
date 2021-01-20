package me.dreamhopping.pml.gradle.data.minecraft

import org.gradle.api.Project

data class Library(val name: String, val natives: Map<String, String>?, val rules: List<Rule>?) {
    private fun getNative(os: String) = natives?.get(os)?.replace(
        "\${arch}",
        if (System.getProperty("os.arch") in X64_ARCHITECTURES) "64" else "32"
    )

    fun allowed(os: String) = rules == null || rules.isEmpty() || rules.lastOrNull { it.appliesTo(os) }?.action == "allow"

    fun addToDependencies(project: Project, configName: String) {
        if (natives != null) {
            for (os in arrayOf("windows", "osx", "linux")) {
                if (allowed(os)) getNative(os)?.let { project.dependencies.add(configName, "$name:$it") }
            }
        } else {
            project.dependencies.add(configName, name)
        }
    }

    companion object {
        private val X64_ARCHITECTURES = arrayOf("amd64")
    }
}