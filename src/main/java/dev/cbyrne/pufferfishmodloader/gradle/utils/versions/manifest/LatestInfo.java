package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest;

public class LatestInfo {
    private final String release;
    private final String snapshot;

    public LatestInfo(String release, String snapshot) {
        this.release = release;
        this.snapshot = snapshot;
    }

    public String getRelease() {
        return release;
    }

    public String getSnapshot() {
        return snapshot;
    }
}
