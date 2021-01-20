package me.dreamhopping.pml.gradle.data.minecraft

data class Rule(val action: String, val os: Os?) {
    fun appliesTo(targetOs: String) = os == null || os.appliesTo(targetOs)

    data class Os(val name: String?) {
        fun appliesTo(os: String): Boolean {
            if (name != null && name != os) return false
            return true
        }
    }
}
