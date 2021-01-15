package me.dreamhopping.pml.gradle.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

fun InputStream.hash(algorithm: String) = MessageDigest.getInstance(algorithm).run {
    readInBlocks { bytes, count ->
        update(bytes, 0, count)
    }

    digest().toHexString()
}

fun File.hash(algorithm: String) = inputStream().use { it.hash(algorithm) }

fun InputStream.sha1() = hash("SHA-1")

fun File.sha1() = hash("SHA-1")