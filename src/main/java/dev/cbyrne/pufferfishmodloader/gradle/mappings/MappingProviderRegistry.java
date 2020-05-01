package dev.cbyrne.pufferfishmodloader.gradle.mappings;

import org.gradle.api.GradleException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MappingProviderRegistry {
    private static final Map<String, Supplier<MappingProvider>> providers = new HashMap<>();

    public static MappingProvider getMappingProvider(String name) {
        Supplier<MappingProvider> supplier = providers.get(name);
        if (supplier == null) {
            throw new GradleException("Invalid mapping provider " + name);
        }
        return supplier.get();
    }

    private static void register(String name, Supplier<MappingProvider> supplier) {
        providers.put(name, supplier);
    }

    static {
        register("yarn", YarnMappingProvider::new);
        register("mcp", McpMappingProvider::new);
        register("mojang", MojangMappingProvider::new);
    }
}
