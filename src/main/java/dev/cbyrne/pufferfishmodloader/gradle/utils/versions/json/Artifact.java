package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

public class Artifact {
    private final String sha1;
    private final int size;
    private final String url;

    public Artifact(String sha1, int size, String url) {
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

    public String getUrl() {
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
