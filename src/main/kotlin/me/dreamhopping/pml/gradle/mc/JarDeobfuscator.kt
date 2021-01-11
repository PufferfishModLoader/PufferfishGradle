package me.dreamhopping.pml.gradle.mc

import me.dreamhopping.pml.gradle.data.TargetExt
import me.dreamhopping.pml.gradle.mappings.MappingInfo
import me.dreamhopping.pml.gradle.mappings.MappingProvider
import me.dreamhopping.pml.gradle.mc.access.PGAccessMap
import net.md_5.specialsource.*
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File

object JarDeobfuscator {
    fun deobf(input: File, output: File, info: MappingInfo, ext: TargetExt, project: Project) {
        output.parentFile?.mkdirs()

        val mapping = JarMapping()
        mapping.classes += info.classes
        mapping.fields += info.fields
        info.methods.forEach { mapping.methods["${it.key.first} ${it.key.second}"] = it.value }
        mapping.packages += info.packages

        val map = PGAccessMap()
        ext.accessTransformers.forEach {
            map.loadAccessTransformer(File(project.projectDir, it))
        }
        val remapper = JarRemapper(RemapperProcessor(null, mapping, map), mapping, null)
        map.maps = remapper

        val inputJar = Jar.init(input)
        val inheritanceProvider = JointProvider()
        inheritanceProvider.add(JarProvider(inputJar))
        mapping.setFallbackInheritanceProvider(inheritanceProvider)

        remapper.remapJar(inputJar, output)
    }
}