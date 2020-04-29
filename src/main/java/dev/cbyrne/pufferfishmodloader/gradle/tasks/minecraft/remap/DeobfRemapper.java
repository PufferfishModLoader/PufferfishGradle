package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.remap;

import dev.cbyrne.pufferfishmodloader.gradle.mappings.MappingProvider;
import dev.cbyrne.pufferfishmodloader.gradle.utils.Triple;
import org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DeobfRemapper extends Remapper {
    /*private final InheritanceProvider inheritanceProvider;
    private final MappingProvider provider;
    private final boolean backwards;
    private final Map<Triple<String, String, String>, String> methodNameCache = new HashMap<>();

    public DeobfRemapper(InheritanceProvider inheritanceProvider, MappingProvider provider, boolean backwards) {
        this.inheritanceProvider = inheritanceProvider;
        this.provider = provider;
        this.backwards = backwards;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        String mapped = provider.mapMethodName(owner, name, descriptor, backwards);
        if (mapped == null) mapped = name;
        return mapped;
    }

    private String mapNameCached(String owner, String name, String descriptor, Function<String, String> mapper) {
        return methodNameCache.computeIfAbsent(new Triple<>(owner, name, descriptor), t -> {
            String mapped = mapName(owner, name, descriptor, mapper);
            if (mapped == null) {
                mapped = name;
            }
            return mapped;
        });
    }

    private String mapName(String owner, String name, String desc, Function<String, String> mapper) {
        String current = mapper.apply(owner);
        if (current == null) {
            List<String> supers = inheritanceProvider.getSupers(owner);
            for (Iterator<String> it = supers.iterator(); current == null && it.hasNext();) {
                String supername = it.next();
                current = mapNameCached(supername, name, desc, mapper);
            }
        }
        return current;
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        return mapNameCached(owner, name, descriptor, o -> provider.mapFieldName(owner, name, descriptor, backwards));
    }

    @Override
    public String map(String internalName) {
        String rv = provider.mapClassName(internalName, backwards);
        if (rv == null) {
            return internalName;
        } else {
            return rv;
        }
    }*/
}
