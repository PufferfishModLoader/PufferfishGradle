package me.dreamhopping.pml.gradle.utils

import java.net.URL
import java.net.URLClassLoader

class BetterURLClassLoader(urls: Array<URL>, private val myParent: ClassLoader) : URLClassLoader(urls, null) {
    override fun loadClass(name: String?): Class<*> {

        return super.loadClass(name)
    }

    override fun findClass(name: String): Class<*> {
        return try {
            super.findClass(name)
        } catch (e: ClassNotFoundException) {
            myParent.loadClass(name)
        }
    }
}