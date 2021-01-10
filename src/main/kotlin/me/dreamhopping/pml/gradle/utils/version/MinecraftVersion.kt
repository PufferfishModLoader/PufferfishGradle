package me.dreamhopping.pml.gradle.utils.version

interface MinecraftVersion {
    operator fun compareTo(other: MinecraftVersion): Int

    companion object {
        private val RELEASE_REGEX = """^(\d+)\.(\d+)(\.(\d+))?$""".toRegex()
        private val PRE_RELEASE_REGEX = """^(\d+)\.(\d+)(\.(\d+))?((-pre)|( Pre-Release )|(-rc))(\d+)$""".toRegex()
        private val SNAPSHOT_REGEX = """^(\d+)w(\d+)([a-z])$""".toRegex()

        fun parse(name: String): MinecraftVersion {
            return PRE_RELEASE_REGEX.matchEntire(name)?.let {
                PreReleaseVersion(
                    it.groupValues[1].toInt(),
                    it.groupValues[2].toInt(),
                    it.groupValues[4].toIntOrNull() ?: 0,
                    it.groupValues[5].toInt()
                )
            } ?: SNAPSHOT_REGEX.matchEntire(name)?.let {
                SnapshotVersion(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3][0])
            } ?: RELEASE_REGEX.matchEntire(name)?.let {
                ReleaseVersion(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[4].toIntOrNull() ?: 0)
            } ?: error("Invalid Minecraft version $name")
        }
    }
}
