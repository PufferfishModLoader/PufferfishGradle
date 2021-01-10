package me.dreamhopping.pml.gradle.utils.http

import me.dreamhopping.pml.gradle.utils.Hash
import java.io.File

object Downloader {
    suspend fun download(url: String, destination: File, sha1: String? = null, maxTries: Int = 5, silent: Boolean = false) {
        var tries = 0
        destination.parentFile?.mkdirs()
        while (!destination.exists() || (sha1 != null && Hash.sha1(destination) != sha1)) {
            if (tries++ >= maxTries) error("Failed to download $url after $maxTries tries")
            if (!silent) println("Downloading $url (try $tries/$maxTries)")
            val response =
                HttpRequest.get(url, "User-Agent" to "PufferfishGradle/${javaClass.`package`.implementationVersion}")
            response.data.use {
                if (response.successful) {
                    destination.outputStream().use { output ->
                        it.copyTo(output)
                    }
                } else {
                    println(it.reader().readText())
                }
            }
        }
    }
}