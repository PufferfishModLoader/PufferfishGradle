package me.dreamhopping.pml.gradle.mc.data.version

data class Artifact(override val sha1: String?, override val size: Long?, override val url: String?) : IArtifact
