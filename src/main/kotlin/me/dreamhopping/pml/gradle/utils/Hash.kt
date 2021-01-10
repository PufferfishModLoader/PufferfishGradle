package me.dreamhopping.pml.gradle.utils

import java.io.*
import java.security.MessageDigest

object Hash {
    private const val bufferSize = 4096

    fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }

    fun MessageDigest.copyFileToMessageDigest(file: File) {
        file.inputStream().use { input ->
            copyInputStreamToMessageDigest(input)
        }
    }

    private fun MessageDigest.copyInputStreamToMessageDigest(stream: InputStream) {
        val buf = ByteArray(bufferSize) { 0 }

        while (true) {
            val i = stream.read(buf)
            if (i < 0) break
            update(buf, 0, i)
        }
    }

    fun hash(method: String, input: InputStream) = MessageDigest.getInstance(method).let {
        it.copyInputStreamToMessageDigest(input)

        it.digest().toHexString()
    }

    fun sha1(input: Serializable) = hash("SHA-1", ByteArrayInputStream(ByteArrayOutputStream().also {
        ObjectOutputStream(it).writeObject(input)
    }.toByteArray()))

    fun sha1(input: InputStream) = hash("SHA-1", input)

    fun sha1(input: File) = input.inputStream().use { sha1(it) }
}