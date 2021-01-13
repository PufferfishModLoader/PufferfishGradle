package me.dreamhopping.pml.gradle.tasks.merge

import org.objectweb.asm.Type
import me.dreamhopping.pml.runtime.annotations.DistributionType
import me.dreamhopping.pml.runtime.annotations.OnlyOn
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InnerClassNode

object ClassMerger {
    fun merge(into: ClassNode, from: ClassNode) {
        mergeFields(into, from)
        mergeMethods(into, from)
        mergeInnerClasses(into, from)
    }

    private fun mergeInnerClasses(into: ClassNode, from: ClassNode) {
        from.innerClasses.forEach { if (!into.hasInnerClass(it)) into.innerClasses.add(it) }
    }

    private fun ClassNode.hasInnerClass(node: InnerClassNode) = innerClasses.any { it.matches(node) }

    private fun InnerClassNode.matches(other: InnerClassNode) = innerName == other.innerName && outerName == other.outerName

    private fun mergeMethods(into: ClassNode, from: ClassNode) {
        val processed = hashSetOf<Pair<String, String>>()

        into.methods.forEach { node ->
            val fromNode = from.methods.find { it.name == node.name && it.desc == node.desc }

            if (fromNode == null) {
                node.visibleAnnotations = node.visibleAnnotations.addAnnotationToList(DistributionType.CLIENT)
            }

            processed += node.name to node.desc
        }

        from.methods.forEach { node ->
            if ((node.name to node.desc) !in processed) {
                node.visibleAnnotations = node.visibleAnnotations.addAnnotationToList(DistributionType.SERVER)
                into.methods.add(node)
                processed += node.name to node.desc
            }
        }
    }

    private fun mergeFields(into: ClassNode, from: ClassNode) {
        val processed = hashSetOf<Pair<String, String>>()

        into.fields.forEach { node ->
            val fromNode = from.fields.find { it.name == node.name && it.desc == node.desc }

            if (fromNode == null) {
                node.visibleAnnotations = node.visibleAnnotations.addAnnotationToList(DistributionType.CLIENT)
            }

            processed += node.name to node.desc
        }

        from.fields.forEach { node ->
            if ((node.name to node.desc) !in processed) {
                node.visibleAnnotations = node.visibleAnnotations.addAnnotationToList(DistributionType.SERVER)
                into.fields.add(node)
                processed += node.name to node.desc
            }
        }
    }

    fun MutableList<AnnotationNode>?.addAnnotationToList(type: DistributionType): MutableList<AnnotationNode> {
        val node = AnnotationNode(Type.getDescriptor(OnlyOn::class.java))
        node.visitEnum("value", Type.getDescriptor(DistributionType::class.java), type.name)
        return this?.apply { add(node) } ?: arrayListOf(node)
    }
}