package me.dreamhopping.pml.gradle.mc.data.version

import me.dreamhopping.pml.gradle.mc.McOs

data class Rule(val action: String, val os: OsRule?) {
    val matchesCurrent: Boolean get() {
        var matches = true
        if (os?.name != null) {
            matches = os.name == McOs.current().text
        }
        return matches
    }
}
