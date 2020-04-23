package dev.cbyrne.pufferfishmodloader.gradle.tasks.modules;

import dev.cbyrne.pufferfishmodloader.gradle.extension.ModuleExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

public class TaskGenerateModuleJson extends DefaultTask {
    private Configuration libraryConfiguration;
    private NamedDomainObjectContainer<ModuleExtension> moduleContainer;
    private File output = new File("output.test.txt");

    @TaskAction
    public void generate() {
        System.out.println("bruhh");
    }

    @InputFiles
    public Configuration getLibraryConfiguration() {
        return libraryConfiguration;
    }

    public void setLibraryConfiguration(Configuration libraryConfiguration) {
        this.libraryConfiguration = libraryConfiguration;
    }

    @Input
    public NamedDomainObjectContainer<ModuleExtension> getModuleContainer() {
        return moduleContainer;
    }

    public void setModuleContainer(NamedDomainObjectContainer<ModuleExtension> extensionContainer) {
        this.moduleContainer = extensionContainer;
    }

    @OutputFile
    public File getOutput() {
        return output;
    }
}
