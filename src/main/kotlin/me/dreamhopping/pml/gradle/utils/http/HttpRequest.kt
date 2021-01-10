package me.dreamhopping.pml.gradle.utils.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class HttpRequest(val url: URL, val method: String, vararg val headers: Pair<String, String>) {
    suspend fun execute() = coroutineScope {
        withContext(Dispatchers.IO) {
            url.openConnection().run {
                this as HttpURLConnection
                requestMethod = method
                headers.forEach { (name, value) ->
                    setRequestProperty(name, value)
                }

                val code = responseCode

                val stream = if (code / 100 == 2) {
                    inputStream
                } else {
                    errorStream
                }

                HttpResponse(code, stream)
            }
        }
    }

    companion object {
        suspend fun get(url: String, vararg headers: Pair<String, String>) = coroutineScope {
            HttpRequest(withContext(
                Dispatchers.IO
            ) { URL(url) }, "GET", *headers
            ).execute()
        }
    }
}