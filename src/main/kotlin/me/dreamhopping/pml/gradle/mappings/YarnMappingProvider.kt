package me.dreamhopping.pml.gradle.mappings

import com.google.gson.JsonArray
import me.dreamhopping.pml.gradle.data.yarn.YarnManifestEntry
import me.dreamhopping.pml.gradle.tasks.download.DownloadTask
import me.dreamhopping.pml.gradle.tasks.map.yarn.DownloadYarnMappingsTask
import me.dreamhopping.pml.gradle.tasks.map.yarn.LoadYarnMappingsTask
import me.dreamhopping.pml.gradle.util.DOWNLOAD_MAPPINGS_BASE_NAME
import me.dreamhopping.pml.gradle.util.FETCH_MAPPING_VERSIONS_BASE_NAME
import me.dreamhopping.pml.gradle.util.Json.fromJson
import me.dreamhopping.pml.gradle.util.LOAD_MAPPINGS_BASE_NAME
import me.dreamhopping.pml.gradle.util.getCachedFile
import org.gradle.api.Project
import java.io.File
import java.net.URL

class YarnMappingProvider(version: String? = null, private val onIdChange: () -> Unit) : MappingProvider {
    private var version = version
        set(v) {
            field = v
            onIdChange()
        }
    override val id get() = "yarn-$version"

    override fun fetchIdFromDiskIfPossible(project: Project, minecraftVersion: String) {
        if (version == null) {
            val versionFile = project.getCachedFile("yarn/$minecraftVersion.json")

            if (versionFile.exists()) {
                consumeManifest(versionFile.fromJson())
            }
        }
    }

    override fun setUpVersionSetupTasks(project: Project, id: String, minecraftVersion: () -> String): Pair<String, () -> File> {
        project.afterEvaluate {
            allYarnProviders.getOrPut(minecraftVersion()) { arrayListOf() }.add(this)
        }
        val name = "$FETCH_MAPPING_VERSIONS_BASE_NAME$id"
        if (version == null && project.tasks.findByPath(name) == null) {
            project.tasks.register(name, DownloadTask::class.java) { task ->
                task.downloadEvenIfNotNecessary = true
                task.output = project.getCachedFile("yarn/${minecraftVersion()}.json")
                task.url = URL("https://meta.fabricmc.net/v1/versions/loader/${minecraftVersion()}")
                task.doLast {
                    val manifest = task.output.fromJson<List<YarnManifestEntry>>()
                    allYarnProviders[minecraftVersion()]?.forEach {
                        it.consumeManifest(manifest)
                    }
                }
            }
        }
        val downloadName = "$DOWNLOAD_MAPPINGS_BASE_NAME$id"
        val downloadTask = project.tasks.register(downloadName, DownloadYarnMappingsTask::class.java) {
            if (version == null) {
                it.dependsOn(name)
                it.manifest = project.getCachedFile("yarn/${minecraftVersion()}.json")
            }
            it.versionOverride = version
        }

        val loadName = "$LOAD_MAPPINGS_BASE_NAME$id"

        val task = project.tasks.register(loadName, LoadYarnMappingsTask::class.java) {
            it.dependsOn(downloadName)
            it.downloadTask = downloadTask.get()
            it.outputBase = project.getCachedFile("mappings/yarn-${minecraftVersion()}").absolutePath
            it.versionProvider = { version ?: "unknown" }
        }

        return loadName to { task.get().getOutputJson() }
    }

    private fun consumeManifest(manifest: List<YarnManifestEntry>) {
        version = manifest.getOrNull(0)?.mappings?.version
    }

    companion object {
        private val allYarnProviders = hashMapOf<String, MutableList<YarnMappingProvider>>()

        fun Project.isYarnAvailable(minecraftVersion: String) = getLatestForVersion(minecraftVersion) != null

        private fun Project.getLatestForVersion(minecraftVersion: String): String? {
            val file = getCachedFile("yarn/$minecraftVersion.json")
            file.parentFile?.mkdirs()

            if (!gradle.startParameter.isOffline) {
                URL("https://meta.fabricmc.net/v1/versions/loader/$minecraftVersion").openConnection().apply {
                    setRequestProperty("User-Agent", "PufferfishGradle/${YarnMappingProvider::class.java.`package`.implementationVersion}")

                    getInputStream().use { file.outputStream().use { out -> it.copyTo(out) } }
                }
            }

            val array = file.fromJson<JsonArray>()
            if (array.size() <= 0) return null
            return array[0].asJsonObject["mappings"].asJsonObject["version"].asString
        }
    }
}