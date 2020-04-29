package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft;

import dev.cbyrne.pufferfishmodloader.gradle.utils.HttpUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.URL;

// note: this doesn't declare inputs/outputs because up-to-date is handled by HttpUtils
public class TaskDownloadJar extends DefaultTask {
    private URL url;
    private String sha1;
    private File dest;

    @TaskAction
    public void download() {
        try {
            HttpUtils.download(url, dest, sha1, 5);
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
