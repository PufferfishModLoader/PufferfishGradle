package me.dreamhopping.pml.gradle.mappings

import com.google.gson.JsonArray
import me.dreamhopping.pml.gradle.data.mappings.Mappings
import me.dreamhopping.pml.gradle.util.*
import org.gradle.api.Project
import java.io.File
import java.util.zip.ZipFile

class YarnMappingProvider(
    private var version: String?,
    private val project: Project,
    private val minecraftVersion: String
) : MappingProvider {
    override val id get() = "yarn-$version"
    override val mappings = Mappings.mappings {  }
        get() {
            if (!haveMappingsBeenLoaded) load(project, minecraftVersion)
            return field
        }

    private var haveMappingsBeenLoaded = false

    init {
        version = getVersion(project, minecraftVersion)
    }

    private fun load(project: Project, minecraftVersion: String) {
        val version = getVersion(project, minecraftVersion)

        val path = buildMavenPath("net.fabricmc", "yarn", version)
        val mapZip = File(project.repoDir, path)
        download("https://maven.fabricmc.net/$path", mapZip)

        haveMappingsBeenLoaded = true
        ZipFile(mapZip).use { zip ->
            val entry = zip.getEntry("mappings/mappings.tiny")
            zip.getInputStream(entry).bufferedReader().use { reader ->
                for (line in reader.lines().skip(1)) {
                    val parts = line.split(" ", "\t")

                    when (parts[0]) {
                        "CLASS" -> mappings.classes[parts[1]] = parts[3]
                        "METHOD" -> mappings.methods["${parts[1]}/${parts[3]}${parts[2]}"] = parts[5]
                        "FIELD" -> mappings.fields["${parts[1]}/${parts[3]}"] = parts[5]
                    }
                }
            }
        }
    }

    private fun getVersion(project: Project, minecraftVersion: String) = version ?: project.getLatestVersion(minecraftVersion) ?: error("No Yarn versions for $minecraftVersion")

    companion object {
        private val String.versionsUrl get() = "https://meta.fabricmc.net/v1/versions/loader/$this"

        fun Project.isYarnAvailable(mcVersion: String) = getLatestVersion(mcVersion) != null

        private fun Project.getLatestVersion(mcVersion: String): String? {
            val versionsFile = dataFile("mappings/yarn/$mcVersion.json")

            return versionsFile.getLatestVersion(mcVersion) ?: versionsFile.let {
                it.downloadVersionInfo(mcVersion)
                it.toLatestVersion()
            }
        }

        private fun File.getLatestVersion(mcVersion: String): String? {
            if (!exists()) downloadVersionInfo(mcVersion)
            return toLatestVersion()
        }

        private fun File.toLatestVersion() = fromJson<JsonArray>()
            .takeIf { it.size() > 0 }
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("mappings")
            ?.get("version")?.asString

        private fun File.downloadVersionInfo(mcVersion: String) {
            download(mcVersion.versionsUrl, this, ignoreInitialState = true)
        }
    }
}