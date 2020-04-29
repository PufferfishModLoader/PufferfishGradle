package dev.cbyrne.pufferfishmodloader.gradle.mappings;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;

public abstract class MappingProvider {
    private boolean loaded = false;

    public abstract void initialize(PufferfishGradle plugin, String mcVersion);

    public void load(PufferfishGradle plugin, String version) {
        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public abstract void checkParamsCorrect(PufferfishGradle plugin, String version);

    public abstract String mapClassName(String original, boolean backwards);

    public abstract String mapFieldName(String owner, String original, String desc, boolean backwards);

    public abstract String mapMethodName(String owner, String original, String desc, boolean backwards);
}
