package me.dreamhopping.pml.gradle.data.minecraft

import org.gradle.api.Project

data class Library(val name: String, val natives: Map<String, String>?, val rules: List<Rule>?) {
    fun getNative(os: String) = natives?.get(os)?.replace(
        "\${arch}",
        if (System.getProperty("os.arch") in X64_ARCHITECTURES) "64" else "32"
    )

    fun allowed() = rules == null || rules.isEmpty() || rules.lastOrNull { it.appliesToCurrent() }?.action == "allow"

    fun addToDependencies(project: Project, configName: String) {
        if (natives != null) {
            for (os in arrayOf("windows", "osx", "linux")) {
                getNative(os)?.let { project.dependencies.add(configName, "$name:$os") }
            }
        } else {
            project.dependencies.add(configName, name)
        }
    }

    companion object {
        private val X64_ARCHITECTURES = arrayOf("amd64")
    }
}