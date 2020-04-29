package dev.cbyrne.pufferfishmodloader.gradle.mappings;

import org.objectweb.asm.commons.Remapper;

public class DescRemapUtil extends Remapper {
    private final MappingProvider provider;
    private final boolean backwards;

    public DescRemapUtil(MappingProvider provider, boolean backwards) {
        this.provider = provider;
        this.backwards = backwards;
    }

    @Override
    public String map(String internalName) {
        return provider.mapClassName(internalName, backwards);
    }
}
