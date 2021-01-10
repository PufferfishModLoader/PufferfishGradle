package me.dreamhopping.pml.gradle.mc

import org.apache.tools.ant.taskdefs.condition.Os

enum class McOs(val text: String) {
    WINDOWS("windows"),
    MAC("osx"),
    LINUX("linux");

    companion object {
        fun current() = when {
            Os.isFamily(Os.FAMILY_MAC) -> MAC
            Os.isFamily(Os.FAMILY_UNIX) -> LINUX
            Os.isFamily(Os.FAMILY_NT) -> WINDOWS
            else -> error("Unsupported operating system")
        }
    }
}