package me.dreamhopping.pml.gradle.tasks.merge

import me.dreamhopping.pml.gradle.tasks.merge.ClassMerger.addAnnotationToList
import me.dreamhopping.pml.runtime.annotations.DistributionType
import org.gradle.workers.WorkAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class MergeAction : WorkAction<MergeParameters> {
    override fun execute() {
        val clientJar = parameters.clientJar.asFile.get()
        val serverJar = parameters.serverJar.asFile.get()
        val outputJar = parameters.outputJar.asFile.get()

        outputJar.parentFile?.mkdirs()

        ZipFile(clientJar).use { client ->
            ZipFile(serverJar).use { server ->
                ZipOutputStream(outputJar.outputStream()).use { output ->
                    val processed = hashSetOf<String>()

                    for (entry in client.entries()) {
                        if (entry.name.endsWith(".class")) {
                            val serverEntry = server.getEntry(entry.name)
                            val clientNode = client.readClassNode(entry)
                            if (serverEntry == null) {
                                clientNode.visibleAnnotations =
                                    clientNode.visibleAnnotations.addAnnotationToList(DistributionType.CLIENT)
                            } else {
                                val serverNode = server.readClassNode(serverEntry)
                                ClassMerger.merge(clientNode, serverNode)
                            }
                            output.putNextEntry(ZipEntry(entry.name))
                            output.write(clientNode.toByteArray())
                            output.closeEntry()
                        } else {
                            output.putNextEntry(ZipEntry(entry.name))
                            client.getInputStream(entry).use { it.copyTo(output) }
                            output.closeEntry()
                        }
                        processed += entry.name
                    }

                    for (entry in server.entries()) {
                        if (entry.name !in processed) {
                            if (entry.name.endsWith(".class")) {
                                val node = server.readClassNode(entry)
                                node.visibleAnnotations =
                                    node.visibleAnnotations.addAnnotationToList(DistributionType.SERVER)
                                output.putNextEntry(ZipEntry(entry.name))
                                output.write(node.toByteArray())
                                output.closeEntry()
                            } else {
                                output.putNextEntry(ZipEntry(entry.name))
                                server.getInputStream(entry).use { it.copyTo(output) }
                                output.closeEntry()
                            }
                            processed += entry.name // make sure there can be no duplicate files in the final jar
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun ZipFile.readClassNode(entry: ZipEntry) = ClassNode().also { node ->
            ClassReader(getInputStream(entry).use { it.readBytes() }).accept(node, 0)
        }

        fun ClassNode.toByteArray(): ByteArray = ClassWriter(0).also { accept(it) }.toByteArray()
    }
}