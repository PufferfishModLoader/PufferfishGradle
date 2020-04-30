package dev.cbyrne.pufferfishmodloader.gradle.extension;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.mappings.MappingProvider;
import dev.cbyrne.pufferfishmodloader.gradle.mappings.MappingProviderRegistry;
import dev.cbyrne.pufferfishmodloader.gradle.mappings.YarnMappingProvider;
import groovy.lang.Closure;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TargetExtension implements Serializable {
    private final String version;
    private transient final PufferfishGradle plugin;
    private String clientMainClass = "dev.cbyrne.pufferfishmodloader.launch.PMLClientMain";
    private String serverMainClass = "dev.cbyrne.pufferfishmodloader.launch.PMLServerMain";
    private MappingProvider mappings;
    private File runDir;
    private List<String> accessTransformers = new ArrayList<>();

    public TargetExtension(String version, PufferfishGradle plugin) {
        this.version = version;
        this.plugin = plugin;
        if (YarnMappingProvider.isAvailable(version)) {
            yarn();
        } else {
            mcp();
        }
        runDir = new File(plugin.getProject().getProjectDir(), "run/" + version);
    }

    public void accessTransformer(String... transformers) {
        accessTransformers.addAll(Arrays.asList(transformers));
    }

    public List<String> getAccessTransformers() {
        return accessTransformers;
    }

    public void clientMainClass(String main) {
        clientMainClass = main;
    }

    public void serverMainClass(String main) {
        serverMainClass = main;
    }

    public void setClientMainClass(String clientMainClass) {
        this.clientMainClass = clientMainClass;
    }

    public void setServerMainClass(String serverMainClass) {
        this.serverMainClass = serverMainClass;
    }

    public String getClientMainClass() {
        return clientMainClass;
    }

    public String getServerMainClass() {
        return serverMainClass;
    }

    public void runDir(Object dir) {
        runDir = plugin.getProject().file(dir);
    }

    public void setRunDir(Object dir) {
        runDir(dir);
    }

    public File getRunDir() {
        return runDir;
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

    public MappingProvider getMappings() {
        return mappings;
    }

    public String getVersion() {
        return version;
    }
}
