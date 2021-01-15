package me.dreamhopping.pml.gradle.util

import org.apache.tools.ant.taskdefs.condition.Os

fun currentMcOs() = when {
    Os.isFamily(Os.FAMILY_WINDOWS) -> "windows"
    Os.isFamily(Os.FAMILY_MAC) -> "osx"
    Os.isFamily(Os.FAMILY_UNIX) -> "linux"
    else -> "unknown"
}