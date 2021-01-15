package me.dreamhopping.pml.gradle.data.minecraft

import me.dreamhopping.pml.gradle.util.currentMcOs

data class Rule(val action: String, val os: Os?) {
    fun appliesToCurrent() = os == null || os.appliesToCurrent()

    data class Os(val name: String?, val version: String?) {
        fun appliesToCurrent(): Boolean {
            if (version != null && !version.toRegex().matches(System.getProperty("os.version"))) return false
            if (name != null && name != currentMcOs()) return false
            return true
        }
    }
}
