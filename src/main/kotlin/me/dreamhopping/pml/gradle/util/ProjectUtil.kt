package me.dreamhopping.pml.gradle.util

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

val Project.java: JavaPluginConvention get() = convention.getPlugin(JavaPluginConvention::class.java)