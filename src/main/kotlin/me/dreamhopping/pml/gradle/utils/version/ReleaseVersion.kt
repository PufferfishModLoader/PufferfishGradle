package me.dreamhopping.pml.gradle.utils.version

interface IReleaseVersion {
    val major: Int
    val minor: Int
    val patch: Int

    fun compareIReleaseVersion(other: IReleaseVersion) =
        major.compareTo(other.major).takeUnless { it == 0 } ?: minor.compareTo(other.minor)
            .takeUnless { it == 0 } ?: patch.compareTo(other.patch)
}

data class ReleaseVersion(override val major: Int, override val minor: Int, override val patch: Int) : MinecraftVersion,
    IReleaseVersion {
    override fun compareTo(other: MinecraftVersion) = when (other) {
        is PreReleaseVersion -> compareIReleaseVersion(other).takeUnless { it == 0 } ?: 1
        is IReleaseVersion -> compareIReleaseVersion(other)
        else -> error("don't know how to compare $this to $other")
    }
}