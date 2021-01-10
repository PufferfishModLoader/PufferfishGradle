package me.dreamhopping.pml.gradle.utils.version

data class SnapshotVersion(val year: Int, val week: Int, val iteration: Char) : MinecraftVersion {
    override fun compareTo(other: MinecraftVersion) = when (other) {
        is SnapshotVersion -> when {
            other.year == year -> when {
                other.week == week -> when {
                    other.iteration == iteration -> 0
                    other.iteration < iteration -> 1
                    else -> -1
                }
                other.week < week -> 1
                else -> -1
            }
            other.year < year -> 1
            else -> -1
        }
        else -> error("don't know how to compare $this to $other")
    }
}