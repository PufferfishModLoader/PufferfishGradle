package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

public class LibraryArtifact extends Artifact {
    private final String path;

    public LibraryArtifact(String sha1, int size, String url, String path) {
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
