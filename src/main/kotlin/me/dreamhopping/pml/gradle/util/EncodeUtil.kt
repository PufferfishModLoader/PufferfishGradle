package me.dreamhopping.pml.gradle.util

fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }