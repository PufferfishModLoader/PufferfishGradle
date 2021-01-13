package me.dreamhopping.pml.gradle.data.version

data class Library(val name: String, val rules: List<Rule>?, val natives: Map<String, String>?) {
    fun getNative() = natives?.get(Rule.OsRule.current())?.replace("\${arch}", getArchValue())

    private fun getArchValue() = "64".takeIf { System.getProperty("os.arch") == "amd64" } ?: "32"

    fun isAllowed(): Boolean {
        var state = "allow".takeIf { rules?.isEmpty() ?: true } ?: "disallow"
        rules?.filter { it.matchesCurrent() }?.forEach {
            state = it.action
        }
        return state == "allow"
    }
}
