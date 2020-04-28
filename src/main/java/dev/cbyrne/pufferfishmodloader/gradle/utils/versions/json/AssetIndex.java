package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

public class AssetIndex extends Artifact {
    private final String id;
    private final int totalSize;

    public AssetIndex(String sha1, int size, String url, String id, int totalSize) {
        super(sha1, size, url);
        this.id = id;
        this.totalSize = totalSize;
    }

    @Override
    public String toString() {
        return "AssetIndex{" +
                "id='" + id + '\'' +
                ", totalSize=" + totalSize +
                '}';
    }

    public String getId() {
        return id;
    }

    public int getTotalSize() {
        return totalSize;
    }
}
