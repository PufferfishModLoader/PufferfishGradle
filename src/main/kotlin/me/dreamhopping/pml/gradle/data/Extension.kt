package me.dreamhopping.pml.gradle.data

import org.gradle.api.Project
import groovy.lang.Closure
import me.dreamhopping.pml.gradle.utils.version.MinecraftVersion
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar

class Extension(val project: Project) {
    val modContainer = project.container(ModExt::class.java, ModExt.Factory(project))
    val targets = hashSetOf<TargetExt>()
    var separateVersionJars = false
    var mainSourceSet: SourceSet =
        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName("main")

    @JvmOverloads
    fun separateVersionJars(value: Boolean = true) {
        if (!value) error("Turning off separating version jars manually is unsupported in PufferfishGradle 2")
        separateVersionJars = true
        targets.forEach {
            createJarTaskForVersion(project, it)
        }
    }

    fun target(vararg versions: String) {
        target(*versions) {}
    }

    fun target(vararg versions: String, closure: TargetExt.() -> Unit) {
        targets += versions.map {
            TargetExt(project, it).apply(closure).apply {
                if (separateVersionJars) {
                    createJarTaskForVersion(project, this)
                }
            }
        }
    }

    fun target(version: String, closure: Closure<*>) {
        val ext = TargetExt(project, version)
        project.configure(ext, closure)
        if (separateVersionJars) {
            createJarTaskForVersion(project, ext)
        }
        targets += ext
    }

    fun mainSourceSet(set: SourceSet) {
        mainSourceSet = set
    }

    fun mod(name: String, closure: ModExt.() -> Unit = {}) {
        modContainer.create(name, closure)
    }

    fun mod(name: String) {
        modContainer.create(name)
    }

    fun mod(name: String, closure: Closure<*>) {
        modContainer.create(name, closure)
    }

    fun mods(closure: Closure<*>) {
        modContainer.configure(closure)
    }

    companion object {
        private fun createJarTaskForVersion(proj: Project, ext: TargetExt) {
            val set = proj.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.maybeCreate(
                TargetExt.getMcSourceSetName(
                    ext.version
                )
            )

            proj.tasks.register(set.jarTaskName, Jar::class.java) {
                it.from(set.output)
                it.archiveClassifier.set(TargetExt.getMcSourceSetName(ext.version))
                it.dependsOn(set.classesTaskName)
                it.group = "build"
            }
        }
    }
}