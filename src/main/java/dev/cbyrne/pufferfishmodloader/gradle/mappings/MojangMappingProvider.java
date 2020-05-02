package dev.cbyrne.pufferfishmodloader.gradle.mappings;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.utils.HttpUtils;
import dev.cbyrne.pufferfishmodloader.gradle.utils.Pair;
import dev.cbyrne.pufferfishmodloader.gradle.utils.Triple;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.Artifact;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.VersionJson;
import net.md_5.specialsource.JarMapping;
import org.apache.xerces.impl.xpath.regex.Match;
import org.gradle.api.GradleException;
import org.objectweb.asm.commons.Remapper;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MojangMappingProvider extends MappingProvider implements Serializable {
    private static final Pattern METHOD_MAPPING_PATTERN = Pattern.compile("^ {4}(\\d+:\\d+:)?([^ ]+) ([^ ]+\\.)?([^ (]+)(\\([^)]*\\))(:\\d+:\\d+)? -> ([^\\n]+)$");
    private static final Pattern PG_DESC_PATTERN = Pattern.compile("( )*([^,)(\\n]+)");
    private String targetVersion;

    @Override
    public void initialize(PufferfishGradle plugin, String mcVersion) {
        targetVersion = mcVersion;
    }

    public void useLatestFrom(String version) {
        targetVersion = version;
    }

    @Override
    public void checkParamsCorrect(PufferfishGradle plugin, String version) {
        VersionJson json = plugin.getVersionJson(targetVersion);
        if (json.getDownloads() == null || json.getDownloads().getClientMappings() == null || json.getDownloads().getServerMappings() == null) {
            throw new GradleException("No Mojang mappings available for this version");
        }
    }

    @Override
    protected void load(PufferfishGradle plugin, String version, JarMapping dest) {
        plugin.getProject().getLogger().warn("By using the Mojang mappings, you acknowledge that your mod will not be open source due to Mojang's licensing on their official mappings. The PufferfishGradle team is not responsible for any project utilizing Mojang's official mappings.");
        VersionJson json = plugin.getVersionJson(targetVersion);
        Artifact clientMappings = json.getDownloads().getClientMappings();
        Artifact serverMappings = json.getDownloads().getServerMappings();
        File clientMappingsFile = new File(plugin.getCacheDir(), "versions/" + targetVersion + "/mappings.client.txt");
        File serverMappingsFile = new File(plugin.getCacheDir(), "versions/" + targetVersion + "/mappings.server.txt");
        try {
            HttpUtils.download(clientMappings.getUrl(), clientMappingsFile, clientMappings.getSha1(), 5);
            HttpUtils.download(serverMappings.getUrl(), serverMappingsFile, serverMappings.getSha1(), 5);
        } catch (IOException e) {
            throw new GradleException("Failed to download mappings", e);
        }
        try {
            processMappingsFile(clientMappingsFile, dest);
            processMappingsFile(serverMappingsFile, dest);
        } catch (IOException e) {
            throw new GradleException("Failed to read mappings", e);
        }
    }

    private void processMappingsFile(File mappings, JarMapping dest) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(mappings))) {
            String line;
            String currentClass = null;
            List<Triple<String, String, String[]>> nonClassLines = new ArrayList<>();
            Map<String, String> classObf = new HashMap<>();
            Remapper descRemapper = new Remapper() {
                @Override
                public String map(String internalName) {
                    return classObf.getOrDefault(internalName, internalName);
                }
            };
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                String[] parts = line.split("[ \t]+");
                if (currentClass != null && parts.length == 5) {
                    nonClassLines.add(new Triple<>(currentClass, line, parts));
                } else {
                    currentClass = parts[2].substring(0, parts[2].length() - 1);
                    currentClass = getInternalName(currentClass);
                    dest.classes.put(getInternalName(currentClass), getInternalName(parts[0]));
                    classObf.put(getInternalName(parts[0]), getInternalName(currentClass));
                }
            }
            for (Triple<String, String, String[]> l : nonClassLines) {
                currentClass = l.getFirst();
                String[] parts = l.getThird();
                Matcher matcher = METHOD_MAPPING_PATTERN.matcher(l.getSecond());
                if (matcher.matches()) {
                    String returnType = matcher.group(2);
                    String methodName = matcher.group(4);
                    String pgDesc = matcher.group(5);
                    String obfName = matcher.group(7);
                    String deobfDesc = buildDescFromProguard(returnType, pgDesc);
                    String obfDesc = descRemapper.mapMethodDesc(deobfDesc);
                    dest.methods.put(currentClass + '/' + obfName + ' ' + obfDesc, methodName);
                } else {
                    String obfName = parts[2];
                    String mappedName = parts[4];
                    dest.fields.put(currentClass + '/' + mappedName, obfName);
                }
            }
        }
    }

    private String getInternalName(String pg) {
        return pg.replace('.', '/');
    }

    private String buildDescFromProguard(String returnType, String pgDesc) {
        StringBuilder desc = new StringBuilder("(");
        Matcher matcher = PG_DESC_PATTERN.matcher(pgDesc);
        while (matcher.find()) {
            desc.append(getDescElementFromProguard(matcher.group(2)));
        }
        desc.append(')');
        desc.append(getDescElementFromProguard(returnType));
        return desc.toString();
    }

    private String getDescElementFromProguard(String proguard) {
        switch (proguard) {
            case "void":
                return "V";
            case "byte":
                return "B";
            case "char":
                return "C";
            case "double":
                return "D";
            case "float":
                return "F";
            case "int":
                return "I";
            case "long":
                return "J";
            case "short":
                return "S";
            case "boolean":
                return "Z";
            default:
                int levels = 0;
                while (proguard.endsWith("[]")) {
                    levels++;
                    proguard = proguard.substring(0, proguard.length() - 2);
                }
                if (levels != 0) {
                    StringBuilder type = new StringBuilder();
                    for (int i = 0; i < levels; i++) {
                        type.append('[');
                    }
                    type.append(getDescElementFromProguard(proguard));
                    return type.toString();
                } else {
                    return 'L' + proguard.replace('.', '/') + ';';
                }
        }
    }
}
