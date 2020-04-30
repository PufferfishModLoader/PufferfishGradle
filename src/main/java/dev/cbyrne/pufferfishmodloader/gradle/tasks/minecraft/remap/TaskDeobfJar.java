package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.remap;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.mappings.MappingProvider;
import net.md_5.specialsource.*;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class TaskDeobfJar extends DefaultTask {
    private PufferfishGradle plugin;
    private String version;
    private File input;
    private MappingProvider mappings;
    private File output;
    private boolean backwards;

    @TaskAction
    public void deobf() throws IOException {
        output.getParentFile().mkdirs();

        JarMapping mapping = new JarMapping();
        mappings.loadMappings(plugin, version, mapping);
        AccessMap accessMap = new AccessMap();
        // TODO: Add access transformer support
        RemapperProcessor processor = new RemapperProcessor(null, mapping, accessMap);
        JarRemapper remapper = new JarRemapper(processor, mapping, null);
        Jar inputJar = Jar.init(input);
        JointProvider inheritanceProvider = new JointProvider();
        inheritanceProvider.add(new JarProvider(inputJar));
        mapping.setFallbackInheritanceProvider(inheritanceProvider);
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
}
