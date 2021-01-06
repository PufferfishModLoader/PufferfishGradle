package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.remap;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.mappings.MappingProvider;
import net.md_5.specialsource.*;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TaskDeobfJar extends DefaultTask {
    @Internal
    private transient PufferfishGradle plugin;
    @Input
    private String version;
    private File input;
    private MappingProvider mappings;
    private File output;
    private boolean backwards;
    private List<String> accessTransformers;

    @TaskAction
    public void deobf() throws IOException {
        output.getParentFile().mkdirs();

        JarMapping mapping = new JarMapping();
        mappings.loadMappings(plugin, version, mapping);
        MappingAccessMap accessMap = new MappingAccessMap();
        JarRemapper remapper = new JarRemapper(new RemapperProcessor(null, mapping, accessMap), mapping, null);
        accessMap.setRemapper(remapper);
        Jar inputJar = Jar.init(input);
        JointProvider inheritanceProvider = new JointProvider();
        inheritanceProvider.add(new JarProvider(inputJar));
        mapping.setFallbackInheritanceProvider(inheritanceProvider);

        for (String accessTransformer : accessTransformers) {
            accessMap.loadAccessTransformer(accessTransformer);
        }

        remapper.remapJar(inputJar, output);
    }

    @InputFile
    public File getInput() {
        return input;
    }

    public void setInput(File input) {
        this.input = input;
    }

    @Input
    public MappingProvider getMappings() {
        return mappings;
    }

    public void setMappings(MappingProvider mappings) {
        this.mappings = mappings;
    }

    @OutputFile
    public File getOutput() {
        return output;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    @Input
    public boolean isBackwards() {
        return backwards;
    }

    public void setBackwards(boolean backwards) {
        this.backwards = backwards;
    }

    public PufferfishGradle getPlugin() {
        return plugin;
    }

    public void setPlugin(PufferfishGradle plugin) {
        this.plugin = plugin;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @InputFiles
    public List<String> getAccessTransformers() {
        return accessTransformers;
    }

    public void setAccessTransformers(List<String> accessTransformers) {
        this.accessTransformers = accessTransformers;
    }
}
