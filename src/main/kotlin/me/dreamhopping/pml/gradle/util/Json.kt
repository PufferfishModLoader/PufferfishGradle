package me.dreamhopping.pml.gradle.util

import com.google.gson.GsonBuilder
import java.io.File
import java.lang.reflect.Type

object Json {
    private val GSON = GsonBuilder().create()

    fun <T> File.fromJson(type: Type): T = bufferedReader().use { GSON.fromJson<T>(it, type) }

    fun <T> T.toJson(output: File) = output.bufferedWriter().use { GSON.toJson(this, it) }

    inline fun <reified T> File.fromJson() = fromJson<T>(type<T>())
}