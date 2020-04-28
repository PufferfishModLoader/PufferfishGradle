package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest;

import com.google.gson.annotations.SerializedName;

public enum VersionType {
    @SerializedName("release") RELEASE,
    @SerializedName("snapshot") SNAPSHOT
}
