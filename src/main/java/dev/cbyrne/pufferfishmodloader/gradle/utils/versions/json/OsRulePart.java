package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import java.util.regex.Pattern;

public class OsRulePart {
    private final OperatingSystem name;
    private final String version;

    public OsRulePart(OperatingSystem name, String version) {
        this.name = name;
        this.version = version;
    }

    @Override
    public String toString() {
        return "OsRulePart{" +
                "name=" + name +
                ", version='" + version + '\'' +
                '}';
    }

    public boolean appliesToCurrent() {
        return name == OperatingSystem.current() && (version == null || Pattern.compile(version).matcher(System.getProperty("os.version")).matches());
    }

    public OperatingSystem getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
