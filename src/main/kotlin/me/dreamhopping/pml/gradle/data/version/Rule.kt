package me.dreamhopping.pml.gradle.data.version

import org.apache.tools.ant.taskdefs.condition.Os

data class Rule(val action: String, val os: OsRule?) {
    fun matchesCurrent() = os?.matchesCurrent() ?: true

    data class OsRule(val name: String?) {
        fun matchesCurrent() = name == current()

        companion object {
            fun current() = when {
                Os.isFamily(Os.FAMILY_NT) -> "windows"
                Os.isFamily(Os.FAMILY_MAC) -> "osx"
                Os.isFamily(Os.FAMILY_UNIX) -> "linux"
                else -> "unknown"
            }
        }
    }
}
