package dev.cbyrne.pufferfishmodloader.gradle.extension;

import java.io.Serializable;

public class ModuleExtension implements Serializable {
    private final String name;
    private String version;

    public ModuleExtension(String id, String version) {
        this.name = id;
        this.version = version;
    }

    public void version(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
