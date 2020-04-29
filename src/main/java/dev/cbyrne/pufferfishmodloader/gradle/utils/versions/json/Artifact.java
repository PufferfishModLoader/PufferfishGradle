package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import java.net.URL;

public class Artifact {
    private final String sha1;
    private final int size;
    private final URL url;

    public Artifact(String sha1, int size, URL url) {
        this.sha1 = sha1;
        this.size = size;
        this.url = url;
    }

    public String getSha1() {
        return sha1;
    }

    public int getSize() {
        return size;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "Artifact{" +
                "sha1='" + sha1 + '\'' +
                ", size=" + size +
                ", url='" + url + '\'' +
                '}';
    }
}
