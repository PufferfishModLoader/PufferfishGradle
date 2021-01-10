package me.dreamhopping.pml.gradle.mc.data.version

interface IArtifact {
    val sha1: String?
    val size: Long?
    val url: String?
}