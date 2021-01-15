package me.dreamhopping.pml.gradle.user

import org.gradle.api.Project

class TargetData(val project: Project, val version: String) {
    override fun hashCode() =
        version.hashCode() // Make sure HashSet<TargetData> can only contain one instance per version

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TargetData

        if (project != other.project) return false
        if (version != other.version) return false

        return true
    }
}