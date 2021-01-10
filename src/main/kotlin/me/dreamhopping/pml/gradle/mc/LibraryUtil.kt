package me.dreamhopping.pml.gradle.mc

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.dreamhopping.pml.gradle.mc.MinecraftSetup.repoDir
import me.dreamhopping.pml.gradle.mc.data.version.IArtifact
import me.dreamhopping.pml.gradle.mc.data.version.Library
import me.dreamhopping.pml.gradle.utils.http.Downloader
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File

object LibraryUtil {
    suspend fun addLibrariesToConfiguration(
        project: Project,
        libraries: Collection<Library>,
        config: Configuration,
        nativesConfig: Configuration
    ) = coroutineScope {
        libraries.filter { rulesAllowLibrary(it) }.map {
            async { addLibraryToConfiguration(project, it, config, nativesConfig) }
        }.awaitAll()
    }

    private fun rulesAllowLibrary(library: Library): Boolean {
        var allowed = library.rules?.isEmpty() ?: true
        library.rules?.forEach {
            if (it.matchesCurrent) {
                allowed = it.action == "allow"
            }
        }
        return allowed
    }

    private suspend fun addLibraryToConfiguration(
        project: Project,
        library: Library,
        config: Configuration,
        nativesConfig: Configuration
    ) {
        val classifier = getClassifier(library)
        val c = (config.takeUnless { classifier != null } ?: nativesConfig)
        if (shouldLetGradleDownload(library)) {
            project.dependencies.add(c.name, library.name + (classifier?.let { ":$it" } ?: ""))
        } else {
            val file = getOutputFile(project, library.name, classifier)
            file.parentFile?.mkdirs()
            Downloader.download(
                library.downloads?.artifact?.url ?: buildUrl(library.name, classifier),
                file,
                library.downloads?.artifact?.sha1
            )
            project.dependencies.add(c.name, project.files(file))
        }
    }

    suspend fun downloadArtifact(artifact: IArtifact, out: File) {
        Downloader.download(artifact.url ?: error("Cannot directly download artifact without url"), out, artifact.sha1)
    }

    fun getOutputFile(project: Project, name: String, classifier: String? = null) =
        File(project.gradle.repoDir, buildNamePart(name, classifier))

    private fun shouldLetGradleDownload(library: Library): Boolean {
        val classifier = getClassifier(library)
        val url = if (classifier != null) {
            library.downloads?.classifiers?.get(classifier)?.url
        } else {
            library.downloads?.artifact?.url
        }
        return url == null || url == buildUrl(library.name, classifier)
    }

    private fun getClassifier(library: Library) =
        library.natives?.get(McOs.current().text)?.replace("\${arch}", getMcArchText())

    private fun getMcArchText() = "64".takeIf { System.getProperty("os.arch") == "amd64" } ?: "32"

    private fun buildUrl(mavenDesc: String, classifier: String? = null) =
        "https://libraries.minecraft.net/${buildNamePart(mavenDesc, classifier)}"

    private fun buildNamePart(mavenDesc: String, classifier: String? = null): String {
        val parts = mavenDesc.split(":")
        val cl = classifier?.let { "-$it" } ?: ""
        return "${
            parts[0].replace(
                '.',
                '/'
            )
        }/${parts[1]}/${parts[2]}/${parts[1]}-${parts[2]}$cl.jar"
    }
}