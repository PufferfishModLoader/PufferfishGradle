package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import com.google.gson.annotations.SerializedName;

public enum OperatingSystem {
    @SerializedName("windows") WINDOWS,
    @SerializedName("osx") MACOS,
    @SerializedName("linux") LINUX,
    UNKNOWN;

    public static OperatingSystem current() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Linux")) {
            return LINUX;
        } else if (os.startsWith("Mac")) {
            return MACOS;
        } else if (os.startsWith("Windows")) {
            return WINDOWS;
        } else {
            return UNKNOWN;
        }
    }
}
