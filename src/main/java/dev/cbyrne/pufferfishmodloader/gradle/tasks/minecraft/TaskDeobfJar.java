package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.mappings.MappingProvider;
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
        if (!mappings.isLoaded()) {
            mappings.load(plugin, version);
        }
        output.getParentFile().mkdirs();
        try (ZipFile in = new ZipFile(input);
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(output))) {
            Enumeration<? extends ZipEntry> entries = in.entries();
            DeobfRemapper map = new DeobfRemapper(mappings, backwards);
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    byte[] data;
                    try (InputStream stream = in.getInputStream(entry)) {
                        data = IOUtils.toByteArray(stream);
                    }
                    ClassReader reader = new ClassReader(data);
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    ClassRemapper remapper = new ClassRemapper(writer, map);
                    reader.accept(remapper, 0);
                    out.putNextEntry(new ZipEntry(map.map(entry.getName().substring(0, entry.getName().length() - 6)) + ".class"));
                    out.write(writer.toByteArray());
                } else {
                    out.putNextEntry(new ZipEntry(entry.getName()));
                    try (InputStream stream = in.getInputStream(entry)) {
                        IOUtils.copy(stream, out);
                    }
                }
            }
        }
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
