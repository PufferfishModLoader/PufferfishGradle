package me.dreamhopping.pml.gradle.utils.version

data class PreReleaseVersion(
    override val major: Int,
    override val minor: Int,
    override val patch: Int,
    val preReleaseIndex: Int
) : MinecraftVersion, IReleaseVersion {
    override fun compareTo(other: MinecraftVersion) = when (other) {
        is PreReleaseVersion -> compareIReleaseVersion(other).takeUnless { it == 0 }
            ?: preReleaseIndex.compareTo(other.preReleaseIndex)
        is IReleaseVersion -> compareIReleaseVersion(other)
        else -> error("don't know how to compare $this to $other")
    }
}
