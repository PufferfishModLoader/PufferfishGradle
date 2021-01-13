package me.dreamhopping.pml.gradle.tasks.map

import me.dreamhopping.pml.gradle.mappings.Mapping
import me.dreamhopping.pml.gradle.tasks.map.fixes.SignatureFixer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.tree.ClassNode
import java.util.zip.ZipFile

class PGRemapper(
    private val map: Mapping,
    private val inheritanceMap: InheritanceMap,
    private val inheritanceFetcher: (String) -> Set<String>?
) : Remapper() {
    override fun map(internalName: String) = map.classes[internalName]?.mappedName ?: internalName

    override fun mapMethodName(owner: String, name: String, descriptor: String) =
        map(owner, name, descriptor) { o, n, d ->
            map.classes[o]?.methods?.get("$n/$d") ?: n
        }

    override fun mapFieldName(owner: String, name: String, descriptor: String) =
        map(owner, name, descriptor) { o, n, _ ->
            map.classes[o]?.fields?.get(n) ?: n
        }

    private fun map(
        owner: String,
        name: String,
        descriptor: String,
        getter: (String, String, String) -> String
    ): String {
        var n = getter(owner, name, descriptor)
        if (n == name) {
            getSuperClasses(owner).forEach {
                n = map(it, name, descriptor, getter)
                if (n != name) return n
            }
        }
        return n
    }

    private fun getSuperClasses(name: String) = inheritanceMap.getOrPut(name) {
        inheritanceFetcher(name) ?: emptySet()
    }

    override fun createSignatureRemapper(signatureVisitor: SignatureVisitor?) = SignatureFixer(signatureVisitor, this)
}

typealias InheritanceMap = MutableMap<String, Set<String>>