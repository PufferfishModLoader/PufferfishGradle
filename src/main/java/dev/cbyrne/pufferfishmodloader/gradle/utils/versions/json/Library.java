package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import java.util.Arrays;
import java.util.Map;

public class Library {
    private final String name;
    private final LibraryDownloadsContainer downloads;
    private final Rule[] rules;
    private final Map<OperatingSystem, String> natives;
    private final ExtractInfo extract;

    public Library(String name, LibraryDownloadsContainer downloads, Rule[] rules, Map<OperatingSystem, String> natives, ExtractInfo extract) {
        this.name = name;
        this.downloads = downloads;
        this.rules = rules;
        this.natives = natives;
        this.extract = extract;
    }

    @Override
    public String toString() {
        return "Library{" +
                "name='" + name + '\'' +
                ", downloads=" + downloads +
                ", rules=" + Arrays.toString(rules) +
                ", natives=" + natives +
                ", extract=" + extract +
                '}';
    }

    public String getName() {
        return name;
    }

    public LibraryDownloadsContainer getDownloads() {
        return downloads;
    }

    public Rule[] getRules() {
        return rules;
    }

    public Map<OperatingSystem, String> getNatives() {
        return natives;
    }

    public ExtractInfo getExtract() {
        return extract;
    }
}
