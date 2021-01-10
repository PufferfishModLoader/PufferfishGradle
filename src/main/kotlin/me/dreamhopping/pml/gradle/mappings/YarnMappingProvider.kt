package me.dreamhopping.pml.gradle.mappings

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import me.dreamhopping.pml.gradle.mc.MinecraftSetup.cacheDir
import me.dreamhopping.pml.gradle.utils.Json
import me.dreamhopping.pml.gradle.utils.http.HttpRequest
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

class YarnMappingProvider : MappingProvider() {
    private var version: String? = null

    fun version(version: String) {
        this.version = version
    }

    override suspend fun addDataToConfiguration(
        project: Project,
        configuration: Configuration,
        requestedVersion: String
    ) {
        val v = getVersion(project, requestedVersion)

        project.repositories.maven { it.setUrl("https://maven.fabricmc.net") }
        project.dependencies.add(configuration.name, mapOf(
            "group" to "net.fabricmc",
            "name" to "yarn",
            "version" to v
        ))
    }

    override suspend fun createId(project: Project, requestedVersion: String): String {
        val v = getVersion(project, requestedVersion)

        return "yarn-$v"
    }

    override fun loadIgnoringCache(project: Project, config: Configuration, requestedVersion: String): MappingInfo {
        val classes = linkedMapOf<String, String>()
        val methods = linkedMapOf<Pair<String, String>, String>()
        val fields = linkedMapOf<String, String>()

        for (f in config) {
            if (f.extension == "jar") {
                ZipFile(f).use { file ->
                    val entry = file.getEntry("mappings/mappings.tiny")
                    file.getInputStream(entry).bufferedReader().use { reader ->
                        reader.lines().skip(1).forEach {
                            val parts = it.split(" ", "\t")

                            when (parts[0]) {
                                "CLASS" -> classes[parts[1]] = parts[3]
                                "METHOD" -> methods["${parts[1]}/${parts[3]}" to parts[2]] = parts[5]
                                "FIELD" -> fields["${parts[1]}/${parts[3]}"] = parts[5]
                            }
                        }
                    }
                }
            }
        }

        return MappingInfo(classes, fields, methods, emptyMap())
    }

    private suspend fun getVersion(project: Project, requestedVersion: String): String {
        this.version?.let { return it }

        val versions = getVersions(project, requestedVersion)

        return versions.get(0).asJsonObject
            .getAsJsonObject("mappings")
            .get("version").asString
    }

    companion object {
        fun isAvailable(project: Project, requestedVersion: String) = runBlocking {
            getVersions(project, requestedVersion).size() > 0
        }

        private suspend fun getVersions(project: Project, requestedVersion: String): JsonArray {
            val versionFile = File(project.gradle.cacheDir, "data/yarn/versions.json")

            try {
                val response = HttpRequest.get(
                    "https://meta.fabricmc.net/v1/versions/loader/$requestedVersion",
                    "User-Agent" to "PufferfishGradle/1.0"
                )

                response.data.use {
                    if (response.successful) {
                        versionFile.parentFile.mkdirs()
                        versionFile.outputStream().use { output ->
                            it.copyTo(output)
                        }
                    }
                }
            } catch (e: IOException) {
                if (!versionFile.exists()) error("No MCP version list found")
            }

            return Json.parse<JsonArray>(versionFile.readText())
        }
    }
}