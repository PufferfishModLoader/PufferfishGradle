package me.dreamhopping.pml.gradle.tasks.decompile

import org.jetbrains.java.decompiler.main.extern.IResultSaver
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResultSaver(private val outputFile: File) : IResultSaver {
    private val outputs = hashMapOf<String, Pair<ZipOutputStream, ExecutorService>>()

    init {
        outputFile.parentFile?.mkdirs()
    }

    override fun saveFolder(path: String?) {

    }

    override fun copyFile(source: String?, path: String?, entryName: String?) {

    }

    override fun saveClassFile(
        path: String?,
        qualifiedName: String?,
        entryName: String?,
        content: String?,
        mapping: IntArray?
    ) {

    }

    override fun createArchive(path: String?, archiveName: String?, manifest: Manifest?) {
        outputs["$path/$archiveName"] = (manifest?.let { JarOutputStream(outputFile.outputStream(), manifest) }
            ?: ZipOutputStream(outputFile.outputStream())) to Executors.newSingleThreadExecutor()
    }

    override fun saveDirEntry(path: String?, archiveName: String?, entryName: String?) {

    }

    override fun copyEntry(source: String?, path: String?, archiveName: String?, entry: String?) {

    }

    override fun saveClassEntry(
        path: String?,
        archiveName: String?,
        qualifiedName: String?,
        entryName: String,
        content: String?
    ) {
        val (zip, service) = outputs["$path/$archiveName"] ?: return
        service.submit {
            zip.putNextEntry(ZipEntry(entryName))
            zip.write((content ?: "").toByteArray())
            zip.closeEntry()
        }
    }

    override fun closeArchive(path: String?, archiveName: String?) {
        val (zip, service) = outputs["$path/$archiveName"] ?: return

        service.apply {
            val future = submit { zip.close() }
            shutdown()
            future.get()
        }

        outputs.remove("$path/$archiveName")
    }
}