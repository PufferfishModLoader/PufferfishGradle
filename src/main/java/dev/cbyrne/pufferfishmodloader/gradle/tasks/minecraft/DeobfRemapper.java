package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft;

import dev.cbyrne.pufferfishmodloader.gradle.mappings.MappingProvider;
import org.objectweb.asm.commons.Remapper;

public class DeobfRemapper extends Remapper {
    private final MappingProvider provider;
    private final boolean backwards;

    public DeobfRemapper(MappingProvider provider, boolean backwards) {
        this.provider = provider;
        this.backwards = backwards;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        return provider.mapMethodName(owner, name, descriptor, backwards);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        return provider.mapFieldName(owner, name, descriptor, backwards);
    }

    @Override
    public String map(String internalName) {
        return provider.mapClassName(internalName, backwards);
    }
}
