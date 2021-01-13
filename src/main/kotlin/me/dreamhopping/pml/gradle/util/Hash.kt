package me.dreamhopping.pml.gradle.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object Hash {
    fun sha1(input: InputStream) = MessageDigest.getInstance("SHA-1").let {
        val buffer = ByteArray(4096)
        while (true) {
            val i = input.read(buffer)
            if (i < 0) break
            it.update(buffer, 0, i)
        }
        it.digest().toHexString()
    }

    fun File.sha1() = inputStream().use { sha1(it) }

    fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }
}