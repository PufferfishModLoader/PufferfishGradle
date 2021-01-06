package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft;

import dev.cbyrne.pufferfishmodloader.gradle.utils.HashUtils;
import dev.cbyrne.pufferfishmodloader.gradle.utils.HttpUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class TaskDownloadJar extends DefaultTask {
    @Input
    private URL url;
    @Input
    private String sha1;
    @OutputFile
    private File dest;

    public TaskDownloadJar() {
        getOutputs().upToDateWhen(task -> {
            try {
                return dest.exists() && sha1.equals(HashUtils.sha1(dest));
            } catch (IOException e) {
                return false;
            }
        });
    }

    @TaskAction
    public void download() {
        try {
            HttpUtils.download(url, dest, sha1, 5, true);
        } catch (IOException e) {
            throw new GradleException("Failed to download Minecraft jar", e);
        }
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public File getDest() {
        return dest;
    }

    public void setDest(File dest) {
        this.dest = dest;
    }
}
