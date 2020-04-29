package dev.cbyrne.pufferfishmodloader.gradle.extension;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.mappings.MappingProvider;
import dev.cbyrne.pufferfishmodloader.gradle.mappings.MappingProviderRegistry;
import dev.cbyrne.pufferfishmodloader.gradle.mappings.YarnMappingProvider;
import groovy.lang.Closure;

public class TargetExtension {
    private final String version;
    private final PufferfishGradle plugin;
    private MappingProvider mappings;

    public TargetExtension(String version, PufferfishGradle plugin) {
        this.version = version;
        this.plugin = plugin;
        if (YarnMappingProvider.isAvailable(version)) {
            yarn();
        } else {
            mcp();
        }
    }

    public void mcp() {
        mappings("mcp");
    }

    public void mcp(Closure<?> closure) {
        mappings("mcp", closure);
    }

    public void yarn() {
        mappings("yarn");
    }

    public void yarn(Closure<?> closure) {
        mappings("yarn", closure);
    }

    public void mappings(String name) {
        MappingProvider provider = MappingProviderRegistry.getMappingProvider(name);
        provider.initialize(plugin, version);
        mappings = provider;
    }

    public void mappings(String name, Closure<?> closure) {
        MappingProvider provider = MappingProviderRegistry.getMappingProvider(name);
        provider.initialize(plugin, version);
        plugin.getProject().configure(provider, closure);
        mappings = provider;
    }

    public void bruh() {
        System.out.println("ok thne");
    }

    public MappingProvider getMappings() {
        return mappings;
    }

    public String getVersion() {
        return version;
    }
}
