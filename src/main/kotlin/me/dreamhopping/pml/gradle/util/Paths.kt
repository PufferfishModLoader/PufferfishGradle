package me.dreamhopping.pml.gradle.util

import org.gradle.api.Project
import java.io.File

val Project.cacheDir get() = File(gradle.gradleUserHomeDir, "caches/pgmc3")
val Project.repoDir get() = File(cacheDir, "repo")

fun Project.getCachedFile(name: String) = File(cacheDir, "data/$name")
fun Project.getRepoFile(
    group: String,
    name: String,
    version: String,
    classifier: String? = null,
    extension: String = "jar"
) =
    File(
        repoDir,
        "${
            group.replace(
                '.',
                '/'
            )
        }/$name/$version/$name-$version${classifier?.let { "-$it" } ?: ""}.$extension")