package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import java.util.List;

public class ExtractInfo {
    private final List<String> exclude;

    public ExtractInfo(List<String> exclude) {
        this.exclude = exclude;
    }

    @Override
    public String toString() {
        return "ExtractInfo{" +
                "exclude=" + exclude +
                '}';
    }

    public List<String> getExclude() {
        return exclude;
    }
}
