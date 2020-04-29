package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.remap;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class InheritanceProvider {
    private final Map<String, List<String>> cache = new HashMap<>();
    private final ZipFile zip;

    public InheritanceProvider(ZipFile zip) {
        this.zip = zip;
    }

    public List<String> getSupers(String className) {
        return cache.computeIfAbsent(className, c -> {
            ZipEntry entry = zip.getEntry(c + ".class");
            if (entry == null) {
                return new ArrayList<>();
            } else {
                List<String> rv = new ArrayList<>();

                byte[] data;
                try (InputStream stream = zip.getInputStream(entry)) {
                    data = IOUtils.toByteArray(stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(data);
                reader.accept(node, 0);

                rv.add(node.superName);
                rv.addAll(node.interfaces);

                return rv;
            }
        });
    }
}
