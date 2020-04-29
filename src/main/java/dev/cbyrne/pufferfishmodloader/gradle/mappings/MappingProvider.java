package dev.cbyrne.pufferfishmodloader.gradle.mappings;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import net.md_5.specialsource.JarMapping;

public abstract class MappingProvider {
    public abstract void initialize(PufferfishGradle plugin, String mcVersion);

    public void load(PufferfishGradle plugin, String version, JarMapping dest) {

    }

    public abstract void checkParamsCorrect(PufferfishGradle plugin, String version);
}
