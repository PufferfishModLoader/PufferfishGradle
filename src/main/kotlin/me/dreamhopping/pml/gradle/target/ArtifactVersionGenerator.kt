package me.dreamhopping.pml.gradle.target

import me.dreamhopping.pml.gradle.mappings.MappingProvider
import me.dreamhopping.pml.gradle.user.TargetData
import me.dreamhopping.pml.gradle.util.readInBlocks
import me.dreamhopping.pml.gradle.util.toHexString
import java.io.File
import java.security.MessageDigest

object ArtifactVersionGenerator {
    fun TargetData.buildMappedJarArtifactVersion() =
        buildMappedJarArtifactVersion(version, mappings, accessTransformers)

    fun buildMappedJarArtifactVersion(
        minecraftVersion: String,
        mappings: List<MappingProvider>,
        accessTransformers: Set<File>
    ) = "$minecraftVersion-${mappings.id}-${accessTransformers.combinedHash}"

    private val Collection<MappingProvider>.id get() = joinToString("-") { it.id }.takeUnless { it.isBlank() } ?: "none"
    private val Collection<File>.combinedHash
        get() = takeIf { it.isNotEmpty() }?.let { list ->
            MessageDigest.getInstance("SHA-1").run {
                list.forEach { file ->
                    file.inputStream().use {
                        it.readInBlocks { bytes, length ->
                            update(bytes, 0, length)
                        }
                    }
                }

                digest().toHexString()
            }
        } ?: "none"
}