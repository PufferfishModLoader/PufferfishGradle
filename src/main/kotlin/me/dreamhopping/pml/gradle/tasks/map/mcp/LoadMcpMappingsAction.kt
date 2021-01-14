package me.dreamhopping.pml.gradle.tasks.map.mcp

import me.dreamhopping.pml.gradle.mappings.Mapping
import me.dreamhopping.pml.gradle.util.Json.toJson
import org.gradle.workers.WorkAction
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

abstract class LoadMcpMappingsAction : WorkAction<LoadMcpMappingsParameters> {
    override fun execute() {
        val mcp = parameters.mcp.asFile.get()
        val srg = parameters.srg.asFile.get()
        val output = parameters.output.asFile.get()
        val isLegacy = parameters.legacy.get()

        val fields = hashMapOf<String, String>()
        val methods = hashMapOf<String, String>()

        ZipFile(mcp).use {
            it.loadCsv(it.getEntry("fields.csv"), fields)
            it.loadCsv(it.getEntry("methods.csv"), methods)
        }

        val mapping = Mapping()

        if (isLegacy) {
            ZipFile(srg).use { zip ->
                zip.getInputStream(zip.getEntry("joined.srg")).bufferedReader().use { reader ->
                    reader.lines().forEach {
                        val parts = it.split(" ")
                        when (parts[0]) {
                            "CL:" -> mapping.classes[parts[1]] = Mapping.ClassMap(parts[2], linkedMapOf(), linkedMapOf())
                            "MD:" -> {
                                val (owner, name) = mapName(parts[3], methods)
                                val (obfOwner, obfName) = extractOwnerAndName(parts[1])
                                mapping.ensureClassMapped(obfOwner, owner).methods["$obfName/${parts[2]}"] = name
                            }
                            "FD:" -> {
                                val (owner, name) = mapName(parts[2], fields)
                                val (obfOwner, obfName) = extractOwnerAndName(parts[1])
                                mapping.ensureClassMapped(obfOwner, owner).fields[obfName] = name
                            }
                        }
                    }
                }
            }
        } else {
            ZipFile(srg).use { zip ->
                zip.getInputStream(zip.getEntry("config/joined.tsrg")).bufferedReader().use { reader ->
                    var currentClass = "" to ""
                    reader.lines().forEach {
                        if (it.startsWith("\t") || it.startsWith(" ")) {
                            val parts = it.trim().split(" ")
                            if (parts.size == 3) {
                                mapping.ensureClassMapped(currentClass.first, currentClass.second)
                                    .methods["${parts[0]}/${parts[1]}"] = methods[parts[2]] ?: parts[2]
                            } else {
                                mapping.ensureClassMapped(currentClass.first, currentClass.second)
                                    .fields[parts[0]] = fields[parts[1]] ?: parts[1]
                            }
                        } else {
                            val parts = it.split(" ")
                            currentClass = parts[0] to parts[1]
                            mapping.classes[parts[0]] = Mapping.ClassMap(parts[1], linkedMapOf(), linkedMapOf())
                        }
                    }
                }
            }
        }

        output.parentFile?.mkdirs()
        mapping.toJson(output)
    }

    private fun extractOwnerAndName(src: String): Pair<String, String> {
        val idx = src.lastIndexOf('/')
        return src.substring(0, idx) to src.substring(idx + 1)
    }

    private fun mapName(src: String, csv: Map<String, String>): Pair<String, String> {
        val (owner, name) = extractOwnerAndName(src)
        return owner to (csv[name] ?: name)
    }

    private fun ZipFile.loadCsv(entry: ZipEntry, map: MutableMap<String, String>) {
        getInputStream(entry).bufferedReader().use { reader ->
            reader.lines().forEach { line ->
                val parts = line.split(",")
                map[parts[0]] = parts[1]
            }
        }
    }
}