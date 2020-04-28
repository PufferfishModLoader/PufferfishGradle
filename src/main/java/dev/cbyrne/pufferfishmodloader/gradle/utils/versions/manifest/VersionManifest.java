package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest;

public class VersionManifest {
    private final LatestInfo latest;
    private final VersionManifestEntry[] versions;

    public VersionManifest(LatestInfo latest, VersionManifestEntry[] versions) {
        this.latest = latest;
        this.versions = versions;
    }

    public LatestInfo getLatest() {
        return latest;
    }

    public VersionManifestEntry[] getVersions() {
        return versions;
    }
}
