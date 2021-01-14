package me.dreamhopping.pml.gradle.tasks.run

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

abstract class GenRunConfigsTask : DefaultTask() {
    @Input
    lateinit var vmArgs: List<String>

    @Input
    lateinit var args: List<String>

    @Input
    lateinit var workDir: String

    @Input
    lateinit var environment: Map<String, () -> String>

    @Input
    lateinit var mainClass: String

    @Input
    lateinit var configName: String

    @Input
    var select = false

    @Internal
    lateinit var sourceSetNameGetter: () -> String

    @Input
    fun getSourceSetName() = sourceSetNameGetter()

    @TaskAction
    fun generate() {
        val file = findWorkspaceFile()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

        val rootElement = findRunManager(doc) ?: createRunManager(doc)
        if (select) rootElement.setAttribute("selected", "Application.$configName")
        val list = rootElement.childNodes

        list.forEach {
            if (it is Element && it.getAttribute("type") == "Application" && it.getAttribute("name") == configName) {
                rootElement.removeChild(it)
            }
        }

        val configRoot = rootElement.addChild(
            "configuration",
            "name" to configName,
            "type" to "Application",
            "factoryName" to "Application"
        )

        File(workDir).mkdirs()
        val model = project.extensions.getByType(IdeaModel::class.java)

        configRoot.addChild("option", "name" to "MAIN_CLASS_NAME", "value" to mainClass)
        configRoot.addChild("option", "name" to "PROGRAM_PARAMETERS", "value" to args.toParamString())
        configRoot.addChild("option", "name" to "VM_PARAMETERS", "value" to vmArgs.toParamString())
        configRoot.addChild("option", "name" to "WORKING_DIRECTORY", "value" to workDir)
        configRoot.addChild("module", "name" to "${model.module.name}.${getSourceSetName()}")
        configRoot.addChild("method", "v" to "2")
            .addChild("option", "name" to "Make", "enabled" to "true")
        configRoot.addChild("envs").apply {
            environment.forEach { (name, value) ->
                addChild("env", "name" to name, "value" to value())
            }
        }

        // remove whitespace lines
        val xp = XPathFactory.newInstance().newXPath()
        val nl = xp.evaluate("//text()[normalize-space(.)='']", doc, XPathConstants.NODESET) as NodeList
        nl.forEach {
            it.parentNode.removeChild(it)
        }

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        transformer.setOutputProperty(OutputKeys.METHOD, "xml")
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

        transformer.transform(DOMSource(doc), StreamResult(file))
    }

    private fun List<String>.toParamString() = joinToString(" ")

    private fun Element.addChild(tag: String, vararg attributes: Pair<String, String>) =
        ownerDocument.createElement(tag).also {
            appendChild(it)
            attributes.forEach { attrib ->
                it.setAttribute(attrib.first, attrib.second)
            }
        }

    private inline fun NodeList.forEach(user: (Node) -> Unit) {
        var i = 0
        while (true) {
            if (i >= length) break
            user(item(i++))
        }
    }

    private fun createRunManager(doc: Document): Element {
        val element = doc.createElement("component")
        element.setAttribute("name", "RunManager")
        element.setAttribute("selected", configName)
        return element
    }

    private fun findRunManager(doc: Document): Element? {
        val list = doc.getElementsByTagName("component")
        list.forEach {
            it as Element
            if (it.getAttribute("name") == "RunManager") {
                return it
            }
        }
        return null
    }

    private fun findWorkspaceFile(): File {
        var file: File? = null
        var root = project.projectDir.canonicalFile
        while (file == null && root != project.rootDir.canonicalFile.parentFile) {
            file = File(root, ".idea/workspace.xml")
            if (!file.exists()) {
                file = null
                for (f in root.listFiles() ?: error("cannot list files in $root")) {
                    if (f.isFile && f.extension == "iws") {
                        file = f
                        break
                    }
                }
            }
            root = root.parentFile ?: break
        }
        return file?.takeIf { it.exists() } ?: error("Could not find IntelliJ workspace file")
    }
}