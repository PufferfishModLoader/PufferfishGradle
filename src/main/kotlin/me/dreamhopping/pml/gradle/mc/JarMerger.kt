package me.dreamhopping.pml.gradle.mc

import me.dreamhopping.pml.runtime.OnlyOn
import me.dreamhopping.pml.runtime.Side
import me.dreamhopping.pml.runtime.Start
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InnerClassNode
import java.io.File
import java.nio.file.attribute.FileTime
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

object JarMerger {
    fun merge(client: File, server: File, output: File) {
        output.parentFile?.mkdirs()
        JarFile(client).use { clientJar ->
            JarFile(server).use { serverJar ->
                JarOutputStream(output.outputStream()).use { outputJar ->
                    val processedEntries = hashSetOf<String>()
                    clientJar.entries().iterator().forEach { clientEntry ->
                        if (clientEntry.name.endsWith(".class")) {
                            val serverEntry = serverJar.getJarEntry(clientEntry.name)
                            val bytes = clientJar.getInputStream(clientEntry).use { it.readBytes() }
                            val node = ClassNode()
                            ClassReader(bytes).accept(node, 0)
                            if (serverEntry != null) {
                                val serverNode = ClassNode()
                                val serverBytes = serverJar.getInputStream(serverEntry).use { it.readBytes() }
                                ClassReader(serverBytes).accept(serverNode, 0)
                                mergeNode(node, serverNode)
                            } else {
                                node.visibleAnnotations = node.visibleAnnotations ?: arrayListOf()
                                node.visibleAnnotations.add(createAnnotation(Side.CLIENT))
                            }
                            outputJar.putNextEntry(copyEntry(clientEntry))
                            outputJar.write(ClassWriter(0).also { node.accept(it) }.toByteArray())
                            outputJar.closeEntry()
                        } else {
                            outputJar.putNextEntry(copyEntry(clientEntry))
                            clientJar.getInputStream(clientEntry).use { it.copyTo(outputJar) }
                            outputJar.closeEntry()
                        }
                        processedEntries.add(clientEntry.name)
                    }

                    serverJar.entries().iterator().forEach { entry ->
                        if (entry.name !in processedEntries) {
                            if (entry.name.endsWith(".class")) {
                                val bytes = serverJar.getInputStream(entry).use { it.readBytes() }
                                val node = ClassNode()
                                ClassReader(bytes).accept(node, 0)
                                node.visibleAnnotations = node.visibleAnnotations ?: arrayListOf()
                                node.visibleAnnotations.add(createAnnotation(Side.SERVER))
                                outputJar.putNextEntry(copyEntry(entry))
                                outputJar.write(ClassWriter(0).also { node.accept(it) }.toByteArray())
                                outputJar.closeEntry()
                            } else {
                                outputJar.putNextEntry(copyEntry(entry))
                                serverJar.getInputStream(entry).use { it.copyTo(outputJar) }
                                outputJar.closeEntry()
                            }
                        }
                    }

                    val extraClasses = arrayOf(
                        Start::class.java,
                        Side::class.java,
                        OnlyOn::class.java
                    ) + Start::class.java.declaredClasses
                    extraClasses.forEach { cl ->
                        val path = cl.name.replace('.', '/') + ".class"
                        val entry = ZipEntry(path)
                        entry.lastModifiedTime = FileTime.fromMillis(System.currentTimeMillis())
                        outputJar.putNextEntry(entry)
                        outputJar.write(javaClass.getResourceAsStream("/$path").use { it.readBytes() })
                        outputJar.closeEntry()
                    }
                }
            }
        }
    }

    private fun mergeNode(client: ClassNode, server: ClassNode) {
        mergeFields(client, server)
        mergeMethods(client, server)
        mergeInnerClasses(client, server)
    }

    private fun mergeInnerClasses(client: ClassNode, server: ClassNode) {
        client.innerClasses.forEach { n ->
            if (server.innerClasses.find { it.matches(n) } == null) {
                server.innerClasses.add(n)
            }
        }
        server.innerClasses.forEach { n ->
            if (client.innerClasses.find { it.matches(n) } == null) {
                client.innerClasses.add(n)
            }
        }
    }

    private fun InnerClassNode.matches(other: InnerClassNode) =
        outerName == other.outerName || innerName == other.innerName

    private fun mergeMethods(client: ClassNode, server: ClassNode) {
        val processed = hashSetOf<Pair<String, String>>()
        client.methods.forEach { clientNode ->
            val serverNode = server.methods.find { clientNode.name == it.name && clientNode.desc == it.desc }
            if (serverNode == null) {
                clientNode.visibleAnnotations = clientNode.visibleAnnotations ?: arrayListOf()
                clientNode.visibleAnnotations.add(createAnnotation(Side.CLIENT))
            }
            processed.add(clientNode.name to clientNode.desc)
        }
        server.methods.filter { (it.name to it.desc) !in processed }.forEach { serverNode ->
            serverNode.visibleAnnotations = serverNode.visibleAnnotations ?: arrayListOf()
            serverNode.visibleAnnotations.add(createAnnotation(Side.SERVER))
        }
    }

    private fun mergeFields(client: ClassNode, server: ClassNode) {
        val processed = hashSetOf<Pair<String, String>>()
        client.fields.forEach { clientNode ->
            val serverNode = server.fields.find { clientNode.name == it.name && clientNode.desc == it.desc }
            if (serverNode == null) {
                clientNode.visibleAnnotations = clientNode.visibleAnnotations ?: arrayListOf()
                clientNode.visibleAnnotations.add(createAnnotation(Side.CLIENT))
            }
            processed.add(clientNode.name to clientNode.desc)
        }
        server.fields.filter { (it.name to it.desc) !in processed }.forEach { serverNode ->
            serverNode.visibleAnnotations = serverNode.visibleAnnotations ?: arrayListOf()
            serverNode.visibleAnnotations.add(createAnnotation(Side.SERVER))
        }
    }

    private fun createAnnotation(side: Side) = AnnotationNode(Type.getDescriptor(OnlyOn::class.java)).also {
        it.visitEnum("value", Type.getDescriptor(Side::class.java), side.name)
        it.visitEnd()
    }

    private fun copyEntry(entry: ZipEntry) = ZipEntry(entry.name).also {
        it.lastModifiedTime = FileTime.fromMillis(System.currentTimeMillis())
    }
}