package me.dreamhopping.pml.gradle.util

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

val GSON = GsonBuilder().create()!!

inline fun <reified T> File.fromJson() = bufferedReader().use { GSON.fromJson<T>(it, object : TypeToken<T>() {}.type) }!!

fun <T> T.toJson(file: File) = file.bufferedWriter().use { GSON.toJson(this, it) }