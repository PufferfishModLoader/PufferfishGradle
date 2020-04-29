package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import java.net.URL;

public class LibraryArtifact extends Artifact {
    private final String path;

    public LibraryArtifact(String sha1, int size, URL url, String path) {
        super(sha1, size, url);
        this.path = path;
    }

    @Override
    public String toString() {
        return "LibraryArtifact{" +
                "path='" + path + '\'' +
                '}';
    }

    public String getPath() {
        return path;
    }
}
