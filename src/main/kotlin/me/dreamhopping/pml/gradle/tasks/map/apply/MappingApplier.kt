package me.dreamhopping.pml.gradle.tasks.map.apply

import me.dreamhopping.pml.gradle.data.mappings.Mappings
import me.dreamhopping.pml.gradle.tasks.map.apply.processors.InheritanceMap
import me.dreamhopping.pml.gradle.tasks.map.apply.processors.SignatureFixer
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.signature.SignatureVisitor

class MappingApplier(private val mappings: Mappings, private val inheritanceMap: InheritanceMap) : Remapper() {
    override fun createSignatureRemapper(signatureVisitor: SignatureVisitor?) = SignatureFixer(signatureVisitor, this)

    override fun map(internalName: String) = mappings.classes[internalName] ?: internalName.takeIf { it.contains('/') }
    ?: "net/minecraft/unmapped/$internalName"

    override fun mapMethodName(owner: String, name: String, descriptor: String) = getName(
        mappings.methods,
        owner,
        name,
        descriptor
    ) ?: name

    override fun mapFieldName(owner: String, name: String, descriptor: String?) = getName(
        mappings.fields,
        owner,
        name,
        null
    ) ?: name

    private fun getName(
        map: Map<String, String>,
        owner: String,
        name: String,
        desc: String?
    ): String? {
        map["$owner/$name${desc ?: ""}"]?.let { return it }
        for (parent in inheritanceMap.getInheritance(owner)) {
            getName(map, parent, name, desc)?.let { return it }
        }
        return null
    }
}