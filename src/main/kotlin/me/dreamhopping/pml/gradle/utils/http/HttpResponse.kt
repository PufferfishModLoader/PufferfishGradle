package me.dreamhopping.pml.gradle.utils.http

import java.io.InputStream

data class HttpResponse(val code: Int, val data: InputStream) {
    val successful get() = code / 100 == 2
}