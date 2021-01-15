package me.dreamhopping.pml.gradle.util

import org.gradle.api.Project
import java.io.File

val Project.cacheDir get() = File(gradle.gradleUserHomeDir, "caches/pgmc3")
val Project.dataDir get() = File(cacheDir, "cachedData")
val Project.repoDir get() = File(cacheDir, "repository")

fun buildMavenPath(
    group: String,
    name: String,
    version: String,
    classifier: String? = null,
    extension: String = "jar"
): String {
    val classifierPart = classifier?.let { "-$it" } ?: ""
    val groupPath = group.replace('.', '/')
    return "$groupPath/$name/$version/$name-$version$classifierPart.$extension"
}

fun Project.dataFile(path: String) = File(dataDir, path)
fun Project.repoFile(
    group: String,
    name: String,
    version: String,
    classifier: String? = null,
    extension: String = "jar"
) = File(repoDir, buildMavenPath(group, name, version, classifier, extension))