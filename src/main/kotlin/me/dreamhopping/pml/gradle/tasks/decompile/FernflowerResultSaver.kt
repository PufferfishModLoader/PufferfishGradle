package me.dreamhopping.pml.gradle.tasks.decompile

import org.jetbrains.java.decompiler.main.extern.IResultSaver
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FernflowerResultSaver(private val outputFile: File) : IResultSaver {
    private val outputs = hashMapOf<String, Pair<ZipOutputStream, ExecutorService>>()

    override fun saveFolder(path: String?) {

    }

    override fun copyFile(source: String?, path: String?, entryName: String?) {

    }

    override fun saveClassFile(
        path: String,
        qualifiedName: String,
        entryName: String,
        content: String,
        mapping: IntArray?
    ) {

    }

    override fun createArchive(path: String, archiveName: String, manifest: Manifest?) {
        val key = "$path/$archiveName"

        outputs[key] = (if (manifest == null) ZipOutputStream(outputFile.outputStream()) else JarOutputStream(
            outputFile.outputStream(),
            manifest
        )) to Executors.newSingleThreadExecutor()
    }

    override fun saveDirEntry(path: String?, archiveName: String?, entryName: String?) {

    }

    override fun copyEntry(source: String, path: String?, archiveName: String?, entry: String?) {

    }

    override fun saveClassEntry(
        path: String,
        archiveName: String,
        qualifiedName: String,
        entryName: String,
        content: String
    ) {
        val pair = outputs["$path/$archiveName"] ?: return
        pair.second.submit {
            pair.first.putNextEntry(ZipEntry(entryName))
            pair.first.write(content.toByteArray())
            pair.first.closeEntry()
        }
    }

    override fun closeArchive(path: String, archiveName: String) {
        val key = "$path/$archiveName"
        val pair = outputs[key] ?: return

        pair.second.let {
            val future = it.submit { pair.first.close() }
            it.shutdown()
            future.get()
        }

        outputs.remove(key)
    }
}