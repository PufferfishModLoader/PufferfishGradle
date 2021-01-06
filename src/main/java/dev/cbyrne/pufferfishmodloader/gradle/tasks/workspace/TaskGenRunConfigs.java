package dev.cbyrne.pufferfishmodloader.gradle.tasks.workspace;

import dev.cbyrne.pufferfishmodloader.Start;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.*;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TaskGenRunConfigs extends DefaultTask {
    private String configName;
    private List<String> vmOptions = new ArrayList<>();
    private List<String> options = new ArrayList<>();
    private String workingDirectory;
    private Map<String, String> environmentVars = new HashMap<>();
    private String sourceSetName;
    private final File outputFile;

    public TaskGenRunConfigs() {
        outputFile = new File(getTemporaryDir(), "temp.xml");
    }

    @TaskAction
    public void generate() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        File file = null;
        try {
            File root = getProject().getProjectDir().getCanonicalFile();
            while (file == null && !root.equals(getProject().getRootDir().getCanonicalFile().getParentFile())) {
                file = new File(root, ".idea/workspace.xml");
                if (!file.exists()) {
                    file = null;
                    for (File f : Objects.requireNonNull(root.listFiles())) {
                        if (f.isFile() && f.getName().endsWith(".iws")) {
                            file = f;
                            break;
                        }
                    }
                }
                File parent = root.getParentFile();
                if (parent == null) break;
                root = parent;
            }
        } catch (IOException ignored) {
        }
        if (file == null || !file.exists()) {
            throw new GradleException("Couldn't find IntelliJ workspace file");
        }

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file);

        Element rootElement = null;
        NodeList list = doc.getElementsByTagName("component");
        for (int i = 0; i < list.getLength(); i++) {
            Element element = (Element) list.item(i);
            if (element.getAttribute("name").equals("RunManager")) {
                rootElement = element;
                break;
            }
        }

        if (rootElement == null) {
            throw new GradleException("Couldn't find run manager");
        }

        list = rootElement.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i) instanceof Element) {
                Element element = (Element) list.item(i);
                if (element.getAttribute("type").equals("Application") && element.getAttribute("name").equals(configName)) {
                    rootElement.removeChild(element);
                }
            }
        }

        Element child = addElement(rootElement,
                "configuration",
                "name", configName,
                "type", "Application",
                "factoryName", "Application",
                "default", "false"
        );

        addElement(child,
                "extension",
                "name", "coverage",
                "enabled", "false",
                "sample_coverage", "true",
                "runner", "idea"
        );

        addElement(child, "option", "name", "MAIN_CLASS_NAME", "value", Start.class.getName());
        addElement(child, "option", "name", "PROGRAM_PARAMETERS", "value", getParamString(options));
        addElement(child, "option", "name", "VM_PARAMETERS", "value", getParamString(vmOptions));

        new File(workingDirectory).mkdirs();
        addElement(child, "option", "name", "WORKING_DIRECTORY", "value", workingDirectory);
        IdeaModel model = (IdeaModel) getProject().getExtensions().getByName("idea");
        addElement(child, "module", "name", model.getModule().getName() + "." + sourceSetName);
        Element child2 = addElement(child, "method", "v", "2");
        addElement(child2, "option", "name", "Make", "enabled", "true");

        Element envs = addElement(child, "envs");
        for (Map.Entry<String, String> entry : environmentVars.entrySet()) {
            addElement(envs, "env", "name", entry.getKey(), "value", entry.getValue());
        }

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);

        transformer.transform(source, result);

        DOMSource source1 = new DOMSource(child);
        StreamResult result1 = new StreamResult(outputFile);

        transformer.transform(source1, result1);
    }

    private String getParamString(List<String> list) {
        StringBuilder rv = new StringBuilder();
        for (String param : list) {
            boolean quote = param.contains(" ");
            if (quote) rv.append('"');
            rv.append(param);
            if (quote) rv.append('"');
            rv.append(' ');
        }
        return rv.toString();
    }

    private Element addElement(Element owner, String name, String... pairs) {
        Document doc = owner.getOwnerDocument();
        if (doc == null) doc = (Document) this;
        Element e = doc.createElement(name);
        for (int i = 0; i < pairs.length; i += 2) {
            e.setAttribute(pairs[i], pairs[i + 1]);
        }
        owner.appendChild(e);
        return e;
    }

    @OutputFile
    public File getOutputFile() {
        return outputFile;
    }

    @Input
    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    @Input
    public List<String> getVmOptions() {
        return vmOptions;
    }

    public void setVmOptions(List<String> vmOptions) {
        this.vmOptions = vmOptions;
    }

    @Input
    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    @Input
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory.getAbsolutePath();
    }

    @Input
    public Map<String, String> getEnvironmentVars() {
        return environmentVars;
    }

    public void setEnvironmentVars(Map<String, String> environmentVars) {
        this.environmentVars = environmentVars;
    }

    @Input
    public String getSourceSetName() {
        return sourceSetName;
    }

    public void setSourceSetName(String sourceSetName) {
        this.sourceSetName = sourceSetName;
    }
}
