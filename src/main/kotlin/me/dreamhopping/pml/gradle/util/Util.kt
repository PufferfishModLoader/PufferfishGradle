package me.dreamhopping.pml.gradle.util

import me.dreamhopping.pml.gradle.PufferfishGradle

val VERSION = PufferfishGradle::class.java.`package`.implementationVersion ?: "unknown"