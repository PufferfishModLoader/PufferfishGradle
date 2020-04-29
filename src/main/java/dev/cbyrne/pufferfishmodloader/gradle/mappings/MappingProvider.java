package dev.cbyrne.pufferfishmodloader.gradle.mappings;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;

public interface MappingProvider {
    void initialize(PufferfishGradle plugin, String mcVersion);

    void load(PufferfishGradle plugin, String version);

    String mapClassName(String original, boolean backwards);

    String mapFieldName(String owner, String original, String desc, boolean backwards);

    String mapMethodName(String owner, String original, String desc, boolean backwards);
}
