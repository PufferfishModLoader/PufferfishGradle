package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft;

import com.google.common.collect.Lists;
import dev.cbyrne.pufferfishmodloader.gradle.sideannotations.OnlyIn;
import dev.cbyrne.pufferfishmodloader.gradle.sideannotations.Side;
import dev.cbyrne.pufferfishmodloader.gradle.utils.ExceptionalSupplier;
import dev.cbyrne.pufferfishmodloader.gradle.utils.IOUtils;
import org.objectweb.asm.Type;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class TaskMergeJars extends DefaultTask {
    private static final Class<?> SIDE_ENUM_CLASS = Side.class;
    private static final Class<?> ONLY_IN_CLASS = OnlyIn.class;

    private File clientJar;
    private File serverJar;
    private File outputJar;

    @TaskAction
    public void merge() {
        try (ZipFile cJar = new ZipFile(clientJar);
             ZipFile sJar = new ZipFile(serverJar);
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputJar))) {
            Set<String> resources = new HashSet<>();
            Map<String, ZipEntry> clientClasses = getClasses(cJar, out, resources);
            Map<String, ZipEntry> serverClasses = getClasses(sJar, out, resources);
            Set<String> added = new HashSet<>();

            for (Map.Entry<String, ZipEntry> entry : clientClasses.entrySet()) {
                String name = entry.getKey();
                ZipEntry entry1 = entry.getValue();
                ZipEntry entry2 = serverClasses.get(name);
                if (entry2 == null) {
                    copyClass(entry1, cJar, out, true);
                    added.add(name);
                    continue;
                }
                serverClasses.remove(name);
                byte[] data1 = getBytes(() -> cJar.getInputStream(entry1));
                byte[] data2 = getBytes(() -> sJar.getInputStream(entry2));
                byte[] data = process(data1, data2);
                ZipEntry entry3 = new ZipEntry(entry1.getName());
                out.putNextEntry(entry3);
                out.write(data);
                added.add(name);
            }

            for (Map.Entry<String, ZipEntry> entry : serverClasses.entrySet()) {
                copyClass(entry.getValue(), sJar, out, false);
            }

            for (String name : Arrays.asList(ONLY_IN_CLASS.getName(), SIDE_ENUM_CLASS.getName())) {
                String name1 = name.replace('.', '/');
                String path = name1 + ".class";
                ZipEntry entry = new ZipEntry(path);
                if (!added.contains(path)) {
                    out.putNextEntry(entry);
                    out.write(getBytes(() -> TaskMergeJars.class.getResourceAsStream("/" + path)));
                }
            }
        } catch (IOException e) {
            throw new GradleException("Failed to merge jarfiles", e);
        }
    }

    private byte[] process(byte[] data1, byte[] data2) {
        ClassNode node1 = getNode(data1);
        ClassNode node2 = getNode(data2);

        processFields(node1, node2);
        processMethods(node1, node2);
        processInnerClasses(node1, node2);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node1.accept(writer);
        return writer.toByteArray();
    }

    private void processFields(ClassNode c1, ClassNode c2) {
        List<FieldNode> f1 = c1.fields;
        List<FieldNode> f2 = c2.fields;

        int serverFieldId = 0;

        for (int clientFieldId = 0; clientFieldId < f1.size(); clientFieldId++) {
            FieldNode cf = f1.get(clientFieldId);
            if (serverFieldId < f2.size()) {
                FieldNode sf = f2.get(serverFieldId);
                if (!cf.name.equals(sf.name)) {
                    boolean found = false;
                    for (int i = serverFieldId + 1; i < f2.size(); i++) {
                        if (cf.name.equals(f2.get(i).name)) {
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        found = false;
                        for (int i = clientFieldId + 1; i < f1.size(); i++) {
                            if (sf.name.equals(f1.get(i).name)) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            if (sf.visibleAnnotations == null) sf.visibleAnnotations = new ArrayList<>();
                            sf.visibleAnnotations.add(createAnnotation(false));
                            f1.add(clientFieldId, sf);
                        }
                    } else {
                        if (cf.visibleAnnotations == null) cf.visibleAnnotations = new ArrayList<>();
                        cf.visibleAnnotations.add(createAnnotation(true));
                        f2.add(serverFieldId, cf);
                    }
                }
            } else {
                if (cf.visibleAnnotations == null) cf.visibleAnnotations = new ArrayList<>();
                cf.visibleAnnotations.add(createAnnotation(true));
                f2.add(serverFieldId, cf);
            }
            serverFieldId++;
        }

        if (f2.size() != f1.size()) {
            for (int x = f1.size(); x < f2.size(); x++) {
                FieldNode sf = f2.get(x);
                if (sf.visibleAnnotations == null) sf.visibleAnnotations = new ArrayList<>();
                sf.visibleAnnotations.add(createAnnotation(true));
                f1.add(x, sf);
            }
        }
    }

    private void processMethods(ClassNode c1, ClassNode c2) {
        List<MethodNode> m1 = c1.methods;
        List<MethodNode> m2 = c2.methods;
        Set<Method> all = new LinkedHashSet<>();

        int pos1 = 0;
        int pos2 = 0;
        int len1 = m1.size();
        int len2 = m2.size();
        String name1 = "";
        String lastName = name1;
        String name2;
        while (pos1 < len1 || pos2 < len2) {
            do {
                if (pos2 >= len2) {
                    break;
                }
                MethodNode sm = m2.get(pos2);
                name2 = sm.name;
                if (!name2.equals(lastName) && pos1 != len1) {
                    break;
                }

                Method m = new Method(sm);
                m.server = true;
                all.add(m);
                pos2++;
            } while (pos2 < len2);

            do {
                if (pos1 >= len1) {
                    break;
                }

                MethodNode cm = m1.get(pos1);
                lastName = name1;
                name1 = cm.name;
                if (!name1.equals(lastName) && pos2 != len2) {
                    break;
                }

                Method m = new Method(cm);
                m.client = true;
                all.add(m);
                pos1++;
            } while (pos1 < len1);
        }

        m1.clear();
        m2.clear();

        for (Method m : all) {
            m1.add(m.node);
            m2.add(m.node);
            if (!(m.server && m.client)) {
                if (m.node.visibleAnnotations == null) m.node.visibleAnnotations = new ArrayList<>();
                m.node.visibleAnnotations.add(createAnnotation(m.client));
            }
        }
    }

    private void processInnerClasses(ClassNode c1, ClassNode c2) {
        List<InnerClassNode> i1 = c1.innerClasses;
        List<InnerClassNode> i2 = c2.innerClasses;
        for (InnerClassNode node : i1) {
            if (doesNotContainInner(i2, node)) {
                i2.add(node);
            }
        }
        for (InnerClassNode node : i2) {
            if (doesNotContainInner(i1, node)) {
                i1.add(node);
            }
        }
    }

    private boolean doesNotContainInner(List<InnerClassNode> list, InnerClassNode node) {
        for (InnerClassNode n : list) {
            if (matches(n, node)) return false;
        }
        return true;
    }

    private boolean matches(InnerClassNode o1, InnerClassNode o2) {
        if (o1.innerName == null && o2.innerName != null) return false;
        if (o1.innerName != null && !o1.innerName.equals(o2.innerName)) return false;
        if (o1.name == null && o2.name != null) return false;
        if (o1.name != null && !o1.name.equals(o2.name)) return false;
        if (o1.outerName == null && o2.outerName != null) {
            return false;
        } else {
            return o1.outerName == null || !o1.outerName.equals(o2.outerName);
        }
    }

    private ClassNode getNode(byte[] data) {
        ClassReader reader = new ClassReader(data);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        return node;
    }

    private void copyClass(ZipEntry entry, ZipFile jar, ZipOutputStream out, boolean client) throws IOException {
        ClassReader reader = new ClassReader(getBytes(() -> jar.getInputStream(entry)));
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        if (node.visibleAnnotations == null) node.visibleAnnotations = new ArrayList<>();
        node.visibleAnnotations.add(createAnnotation(client));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        ZipEntry entry1 = new ZipEntry(entry.getName());
        out.putNextEntry(entry1);
        out.write(writer.toByteArray());
    }

    private AnnotationNode createAnnotation(boolean client) {
        AnnotationNode node = new AnnotationNode(Type.getDescriptor(ONLY_IN_CLASS));
        String[] arr = new String[2];
        arr[0] = Type.getDescriptor(SIDE_ENUM_CLASS);
        if (client) {
            arr[1] = "CLIENT";
        } else {
            arr[1] = "SERVER";
        }
        node.values = Lists.newArrayList("value", arr);
        return node;
    }

    private byte[] getBytes(ExceptionalSupplier<InputStream, IOException> stream) throws IOException {
        try (InputStream in = stream.get()) {
            return org.apache.commons.io.IOUtils.toByteArray(in);
        }
    }

    private Map<String, ZipEntry> getClasses(ZipFile file, ZipOutputStream out, Set<String> resources) throws IOException {
        Map<String, ZipEntry> rv = new HashMap<>();

        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("META-INF/")) continue;
            if (entry.isDirectory()) continue;
            if (!name.endsWith(".class") || name.startsWith(".")) {
                if (!resources.contains(name)) {
                    ZipEntry entry1 = new ZipEntry(name);
                    out.putNextEntry(entry1);
                    try (InputStream stream = file.getInputStream(entry)) {
                        IOUtils.copy(stream, out);
                    }
                    resources.add(name);
                }
            } else {
                rv.put(name.replace(".class", ""), entry);
            }
        }

        return rv;
    }

    @InputFile
    public File getClientJar() {
        return clientJar;
    }

    public void setClientJar(File clientJar) {
        this.clientJar = clientJar;
    }

    @InputFile
    public File getServerJar() {
        return serverJar;
    }

    public void setServerJar(File serverJar) {
        this.serverJar = serverJar;
    }

    @OutputFile
    public File getOutputJar() {
        return outputJar;
    }

    public void setOutputJar(File outputJar) {
        this.outputJar = outputJar;
    }

    private static class Method {
        private final MethodNode node;
        private boolean client = false;
        private boolean server = false;

        public Method(MethodNode node) {
            this.node = node;
        }

        @Override
        public int hashCode() {
            return Objects.hash(node.name, node.desc);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Method)) {
                return false;
            }
            Method other = (Method) obj;
            if (node.name.equals(other.node.name) && node.desc.equals(other.node.desc)) {
                other.client |= client;
                other.server |= server;
                client |= other.client;
                server |= other.server;
            }
            return node.name.equals(other.node.name) && node.desc.equals(other.node.desc);
        }
    }
}
