package me.dreamhopping.pml.gradle.util

import java.io.File
import java.net.URL

val USER_AGENT = "PufferfishGradle/$VERSION"

fun download(url: String, output: File, sha1: String? = null, maxTries: Int = 5, ignoreInitialState: Boolean = false) {
    sha1?.takeIf { !ignoreInitialState && output.exists() && output.sha1() == it }?.let { return }
    if (!ignoreInitialState && sha1 == null && output.exists()) return
    val urlObj = URL(url)
    var ex: Exception? = null
    repeat(maxTries) {
        try {
            println("Attempting download of $url to $output")
            output.parentFile?.mkdirs()
            output.outputStream().use { output ->
                urlObj.openConnection().apply {
                    setRequestProperty("User-Agent", USER_AGENT)

                    getInputStream().use { it.copyTo(output) }
                }
            }

            sha1?.takeIf { !output.exists() || output.sha1() != it }
                ?.let { error("Downloaded file does not match checksum") }
            return
        } catch (e: Exception) {
            ex = e
        }
    }
    ex?.let { throw IllegalStateException("Failed to download $url", it) }
    error("Failed to download $url")
}