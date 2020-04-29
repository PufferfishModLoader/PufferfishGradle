package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

public class TaskMergeJars extends DefaultTask {
    private File clientJar;
    private File serverJar;
    private File outputJar;

    @TaskAction
    public void merge() {

    }

    @InputFile
    public File getClientJar() {
        return clientJar;
    }

    public void setClientJar(File clientJar) {
        this.clientJar = clientJar;
    }

    @InputFile
    public File getServerJar() {
        return serverJar;
    }

    public void setServerJar(File serverJar) {
        this.serverJar = serverJar;
    }

    @OutputFile
    public File getOutputJar() {
        return outputJar;
    }

    public void setOutputJar(File outputJar) {
        this.outputJar = outputJar;
    }
}
