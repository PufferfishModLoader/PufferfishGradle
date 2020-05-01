package dev.cbyrne.pufferfishmodloader.gradle.mappings;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import net.md_5.specialsource.JarMapping;

public abstract class MappingProvider {
    private JarMapping cache;

    public abstract void initialize(PufferfishGradle plugin, String mcVersion);

    public final void loadMappings(PufferfishGradle plugin, String version, JarMapping dest) {
        if (cache == null) {
            cache = new JarMapping();
            load(plugin, version, cache);
        }
        dest.classes.putAll(cache.classes);
        dest.methods.putAll(cache.methods);
        dest.fields.putAll(cache.fields);
        dest.packages.putAll(cache.packages);
    }

    protected abstract void load(PufferfishGradle plugin, String version, JarMapping dest);

    public abstract void checkParamsCorrect(PufferfishGradle plugin, String version);
}
