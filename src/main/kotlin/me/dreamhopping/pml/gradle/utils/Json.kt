package me.dreamhopping.pml.gradle.utils

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object Json {
    val GSON = GsonBuilder().setPrettyPrinting().create()

    inline fun <reified T> parse(json: String) = GSON.fromJson<T>(json, object : TypeToken<T>() {}.type)
}