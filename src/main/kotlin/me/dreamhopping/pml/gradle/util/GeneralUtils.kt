package me.dreamhopping.pml.gradle.util

import com.google.gson.reflect.TypeToken
import org.gradle.api.Project
import org.gradle.api.Task
import java.lang.reflect.Type

inline fun <reified T> Any.cast() = this as T
inline fun <reified T> type(): Type = object : TypeToken<T>() {}.type
inline operator fun <reified T : Task> Project.get(name: String) = tasks.getByName(name).cast<T>()