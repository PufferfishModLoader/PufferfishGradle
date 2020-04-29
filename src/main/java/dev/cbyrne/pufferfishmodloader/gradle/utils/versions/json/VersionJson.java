package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest.VersionType;

import java.util.Arrays;
import java.util.Date;

public class VersionJson {
    private final String id;
    private final String mainClass;
    private final String assets;
    private final String minecraftArguments;

    @Override
    public String toString() {
        return "VersionJson{" +
                "id='" + id + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", assets='" + assets + '\'' +
                ", minimumLauncherVersion=" + minimumLauncherVersion +
                ", releaseTime=" + releaseTime +
                ", time=" + time +
                ", type=" + type +
                ", arguments=" + arguments +
                ", assetIndex=" + assetIndex +
                ", libraries=" + Arrays.toString(libraries) +
                '}';
    }

    private final int minimumLauncherVersion;
    private final Date releaseTime;
    private final Date time;
    private final VersionType type;
    private final ArgumentsContainer arguments;
    private final AssetIndex assetIndex;
    private final Library[] libraries;
    private final DownloadsContainer downloads;

    public VersionJson(String id, String minecraftArguments, String mainClass, String assets, int minimumLauncherVersion, Date releaseTime, Date time, VersionType type, ArgumentsContainer arguments, AssetIndex assetIndex, Library[] libraries, DownloadsContainer downloads) {
        this.id = id;
        this.mainClass = mainClass;
        this.minecraftArguments = minecraftArguments;
        this.assets = assets;
        this.minimumLauncherVersion = minimumLauncherVersion;
        this.releaseTime = releaseTime;
        this.time = time;
        this.type = type;
        this.arguments = arguments;
        this.assetIndex = assetIndex;
        this.libraries = libraries;
        this.downloads = downloads;
    }

    public String getMinecraftArguments() {
        return minecraftArguments;
    }

    public String getId() {
        return id;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getAssets() {
        return assets;
    }

    public int getMinimumLauncherVersion() {
        return minimumLauncherVersion;
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public Date getTime() {
        return time;
    }

    public VersionType getType() {
        return type;
    }

    public ArgumentsContainer getArguments() {
        return arguments;
    }

    public AssetIndex getAssetIndex() {
        return assetIndex;
    }

    public Library[] getLibraries() {
        return libraries;
    }

    public DownloadsContainer getDownloads() {
        return downloads;
    }
}
