package me.dreamhopping.pml.gradle.mc

import me.dreamhopping.pml.gradle.mappings.MappingInfo
import me.dreamhopping.pml.gradle.mappings.MappingProvider
import net.md_5.specialsource.*
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File

object JarDeobfuscator {
    fun deobf(input: File, output: File, info: MappingInfo) {
        output.parentFile?.mkdirs()

        val mapping = JarMapping()
        mapping.classes += info.classes
        mapping.fields += info.fields
        info.methods.forEach { mapping.methods["${it.key.first} ${it.key.second}"] = it.value }
        mapping.packages += info.packages

        // TODO: Implement access transformers
        val remapper = JarRemapper(RemapperProcessor(null, mapping, AccessMap()), mapping, null)

        val inputJar = Jar.init(input)
        val inheritanceProvider = JointProvider()
        inheritanceProvider.add(JarProvider(inputJar))
        mapping.setFallbackInheritanceProvider(inheritanceProvider)

        remapper.remapJar(inputJar, output)
    }
}