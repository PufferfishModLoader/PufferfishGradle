package me.dreamhopping.pml.gradle.util

import java.io.InputStream

inline fun InputStream.readInBlocks(blockSize: Int = 4096, callback: (ByteArray, Int) -> Unit) =
    ByteArray(blockSize).also {
        while (true) {
            val i = read(it)
            if (i < 0) break
            callback(it, i)
        }
    }