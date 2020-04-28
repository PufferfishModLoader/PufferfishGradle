package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.typeadapters.ArgumentTypeAdapter;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest.VersionType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;

public class VersionJson {
    private final String id;
    private final String mainClass;
    private final String assets;

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

    public VersionJson(String id, String mainClass, String assets, int minimumLauncherVersion, Date releaseTime, Date time, VersionType type, ArgumentsContainer arguments, AssetIndex assetIndex, Library[] libraries) {
        this.id = id;
        this.mainClass = mainClass;
        this.assets = assets;
        this.minimumLauncherVersion = minimumLauncherVersion;
        this.releaseTime = releaseTime;
        this.time = time;
        this.type = type;
        this.arguments = arguments;
        this.assetIndex = assetIndex;
        this.libraries = libraries;
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
}
