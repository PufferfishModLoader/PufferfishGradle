package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import java.util.Map;

public class LibraryDownloadsContainer {
    private final LibraryArtifact artifact;
    private final Map<String, LibraryArtifact> classifiers;

    public LibraryDownloadsContainer(LibraryArtifact artifact, Map<String, LibraryArtifact> classifiers) {
        this.artifact = artifact;
        this.classifiers = classifiers;
    }

    @Override
    public String toString() {
        return "LibraryDownloadsContainer{" +
                "artifact=" + artifact +
                ", classifiers=" + classifiers +
                '}';
    }

    public LibraryArtifact getArtifact() {
        return artifact;
    }

    public Map<String, LibraryArtifact> getClassifiers() {
        return classifiers;
    }
}
