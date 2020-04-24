package dev.cbyrne.pufferfishmodloader.gradle.extension;

import org.gradle.api.artifacts.Configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModExtension implements Serializable {
    private final String name;
    private String version;
    private final List<String> dependencies = new ArrayList<>();
    private final List<String> optionalDependencies = new ArrayList<>();
    private final List<String> incompatibilities = new ArrayList<>();
    private final List<String> loadBefore = new ArrayList<>();

    public ModExtension(String id, String version) {
        this.name = id;
        this.version = version;
    }

    public void dependsOn(String... mods) {
        dependencies.addAll(Arrays.asList(mods));
    }

    public void loadAfter(String... mods) {
        optionalDependencies.addAll(Arrays.asList(mods));
    }

    public void incompatibleWith(String... mods) {
        incompatibilities.addAll(Arrays.asList(mods));
    }

    public void loadBefore(String... mods) {
        loadBefore.addAll(Arrays.asList(mods));
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

    public List<String> getDependencies() {
        return dependencies;
    }

    public List<String> getOptionalDependencies() {
        return optionalDependencies;
    }

    public List<String> getIncompatibilities() {
        return incompatibilities;
    }

    public List<String> getLoadBefore() {
        return loadBefore;
    }
}
