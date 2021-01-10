package me.dreamhopping.pml.gradle.utils

import com.google.gson.GsonBuilder

object Json {
    val GSON = GsonBuilder().setPrettyPrinting().create()

    fun <T> parse(json: String, cl: Class<T>) = GSON.fromJson(json, cl)

    inline fun <reified T> parse(json: String) = parse(json, T::class.java)
}