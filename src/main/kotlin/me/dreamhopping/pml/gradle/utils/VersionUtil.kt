package me.dreamhopping.pml.gradle.utils

import me.dreamhopping.pml.gradle.utils.version.IReleaseVersion
import me.dreamhopping.pml.gradle.utils.version.MinecraftVersion
import me.dreamhopping.pml.gradle.utils.version.SnapshotVersion

object VersionUtil {
    fun usesLwjgl3(version: MinecraftVersion) = version is IReleaseVersion && version.minor >= 13
            || version is SnapshotVersion && version >= SnapshotVersion(17, 43, 'a')
}