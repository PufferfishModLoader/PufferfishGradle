package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest;

import java.net.URL;
import java.util.Date;

public class VersionManifestEntry {
    private final String id;
    private final VersionType type;
    private final URL url;
    private final Date time;
    private final Date releaseTime;

    public VersionManifestEntry(String id, VersionType type, URL url, Date time, Date releaseTime) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.time = time;
        this.releaseTime = releaseTime;
    }

    public String getId() {
        return id;
    }

    public VersionType getType() {
        return type;
    }

    public URL getUrl() {
        return url;
    }

    public Date getTime() {
        return time;
    }

    public Date getReleaseTime() {
        return releaseTime;
    }
}
