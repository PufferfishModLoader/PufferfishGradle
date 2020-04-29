package dev.cbyrne.pufferfishmodloader.gradle.mappings;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.utils.Constants;
import dev.cbyrne.pufferfishmodloader.gradle.utils.InputStreamConsumer;
import dev.cbyrne.pufferfishmodloader.gradle.utils.Pair;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class McpMappingProvider extends MappingProvider implements Serializable {
    private static final Pattern MC_VERSION_PATTERN = Pattern.compile("[0-9]+\\.([0-9]+)(\\.[0-9]+)?");

    private final BiMap<String, String> classNames = HashBiMap.create();
    // (owner -> ((officialMethodName, officialMethodDesc) <-> (mappedMethodName, mappedMethodDesc)))
    private final Map<String, BiMap<Pair<String, String>, Pair<String, String>>> methodNames = Maps.newHashMap();
    // (owner -> (officialFieldName <-> mappedFieldName)
    private final Map<String, BiMap<String, String>> fieldNames = Maps.newHashMap();

    private String channel;
    private String version;
    private String actualTargetVersion;

    @Override
    public void initialize(PufferfishGradle plugin, String mcVersion) {
        useLatestFrom(mcVersion);
    }

    public void useLatestFrom(String mcVersion) {
        Pair<String, Integer> latest = getLatest(mcVersion);
        if (latest != null) {
            channel = latest.getFirst();
            version = Integer.toString(latest.getSecond());
        } else {
            channel = null;
            version = null;
        }
    }

    public void channel(String channel) {
        this.channel = channel;
    }

    public void setChannel(String channel) {
        channel(channel);
    }

    public void version(String version) {
        this.version = version;
    }

    public void setVersion(String version) {
        version(version);
    }

    @Override
    public void load(PufferfishGradle plugin, String version) {
        super.load(plugin, version);
        Configuration srg = plugin.getProject().getConfigurations().create(Constants.INTERMEDIARY_CONFIGURATION_NAME + version);
        Configuration mcp = plugin.getProject().getConfigurations().create(Constants.MAPPINGS_CONFIGURATION_NAME + version);
        plugin.getProject().getRepositories().maven(maven -> maven.setUrl("https://files.minecraftforge.net/maven"));
        plugin.getProject().getDependencies().add(mcp.getName(), ImmutableMap.of(
                "group", "de.oceanlabs.mcp",
                "name", "mcp_" + channel,
                "version", this.version,
                "ext", "zip"
        ));

        Matcher matcher = MC_VERSION_PATTERN.matcher(actualTargetVersion);
        if (!matcher.find()) throw new GradleException("Should be impossible");
        int minorVersion = Integer.parseInt(matcher.group(1));

        Map<String, String> fieldNameCsv = new HashMap<>();
        Map<String, String> methodNameCsv = new HashMap<>();

        for (File f : mcp) {
            if (f.getName().endsWith(".zip")) {
                try (ZipFile zip = new ZipFile(f)) {
                    loopNames(zip, "fields.csv", fieldNameCsv::put);
                    loopNames(zip, "methods.csv", methodNameCsv::put);
                } catch (IOException e) {
                    throw new GradleException("Couldn't read mappings", e);
                }
            }
        }

        if (minorVersion < 13) {
            plugin.getProject().getDependencies().add(srg.getName(), ImmutableMap.of(
                    "group", "de.oceanlabs.mcp",
                    "name", "mcp",
                    "version", actualTargetVersion,
                    "classifier", "srg",
                    "ext", "zip"
            ));
            for (File f : srg) {
                if (f.getName().endsWith(".zip")) {
                    useEntryFromZip(f, "joined.srg", stream -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split(" ");
                                if (parts[0].equalsIgnoreCase("CL:")) {
                                    classNames.put(parts[1], parts[2]);
                                } else if (parts[0].equalsIgnoreCase("FD:")) {
                                    Pair<String, String> parts1 = getOwnerAndName(parts[1]);
                                    String owner = parts1.getFirst();
                                    String name = parts1.getSecond();
                                    parts1 = getOwnerAndName(parts[2]);
                                    fieldNames.computeIfAbsent(owner, p -> HashBiMap.create())
                                            .put(name, fieldNameCsv.getOrDefault(parts1.getSecond(), parts1.getSecond()));
                                } else if (parts[0].equalsIgnoreCase("MD:")) {
                                    Pair<String, String> parts1 = getOwnerAndName(parts[1]);
                                    String owner = parts1.getFirst();
                                    String name = parts1.getSecond();
                                    String desc = parts[2];
                                    parts1 = getOwnerAndName(parts[3]);
                                    String descMapped = parts[4];
                                    methodNames.computeIfAbsent(owner, p -> HashBiMap.create())
                                            .put(new Pair<>(name, desc), new Pair<>(methodNameCsv.getOrDefault(parts1.getSecond(), parts1.getSecond()), descMapped));
                                }
                            }
                        }
                    });
                    break;
                }
            }
        } else {
            plugin.getProject().getDependencies().add(srg.getName(), ImmutableMap.of(
                    "group", "de.oceanlabs.mcp",
                    "name", "mcp_config",
                    "version", actualTargetVersion,
                    "ext", "zip"
            ));
            for (File f : srg) {
                if (f.getName().endsWith(".zip")) {
                    useEntryFromZip(f, "config/joined.tsrg", stream -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                            String line;
                            List<Pair<String, String[]>> nonClassLines = new ArrayList<>(); // we need all classes first for desc remapping
                            String currentOwner = null;
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split("[ \t]+");
                                if (parts.length == 2) {
                                    currentOwner = parts[0];
                                    classNames.put(parts[0], parts[1]);
                                } else {
                                    nonClassLines.add(new Pair<>(currentOwner, parts));
                                }
                            }

                            DescRemapUtil remap = new DescRemapUtil(this, false);
                            for (Pair<String, String[]> partsPair : nonClassLines) {
                                String[] parts = partsPair.getSecond();
                                if (parts.length == 3) {
                                    // it's a field
                                    fieldNames.computeIfAbsent(partsPair.getFirst(), c -> HashBiMap.create())
                                            .put(parts[1], fieldNameCsv.getOrDefault(parts[2], parts[2]));
                                } else if (parts.length == 4) {
                                    // it's a method
                                    methodNames.computeIfAbsent(partsPair.getFirst(), c -> HashBiMap.create())
                                            .put(
                                                    new Pair<>(parts[1], parts[2]),
                                                    new Pair<>(methodNameCsv.getOrDefault(parts[3], parts[3]), remap.mapMethodDesc(parts[2]))
                                            );
                                }
                            }
                        }
                    });
                    break;
                }
            }
        }
    }

    @Override
    public void checkParamsCorrect(PufferfishGradle plugin, String version) {
        if (channel == null || this.version == null) throw new GradleException("Invalid MCP version");
        JsonObject versions = getVersions();
        boolean found = false;
        actualTargetVersion = null;
        for (Map.Entry<String, JsonElement> entry : versions.entrySet()) {
            if (found) break;
            JsonObject obj = entry.getValue().getAsJsonObject();
            JsonArray versions0 = obj.getAsJsonArray(channel);
            if (versions0 != null) {
                for (JsonElement elem : versions0) {
                    if (Integer.toString(elem.getAsInt()).equals(this.version)) {
                        this.version += '-' + entry.getKey();
                        found = true;
                        actualTargetVersion = entry.getKey();
                        break;
                    }
                }
            }
        }
        if (!found) {
            throw new GradleException("Invalid MCP version");
        }
    }

    private void loopNames(ZipFile zip, String entryName, BiConsumer<String, String> consumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(zip.getEntry(entryName))))) {
            @SuppressWarnings("UnusedAssignment") String line = reader.readLine(); // discard first line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                consumer.accept(parts[0], parts[1]);
            }
        }
    }

    private Pair<String, String> getOwnerAndName(String entry) {
        int index = entry.lastIndexOf('/');
        return new Pair<>(entry.substring(0, index), entry.substring(index + 1));
    }

    private void useEntryFromZip(File file, String entry, InputStreamConsumer consumer) {
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry entryObj = zip.getEntry(entry);
            try (InputStream stream = zip.getInputStream(entryObj)) {
                consumer.accept(stream);
            }
        } catch (IOException e) {
            throw new GradleException("Couldn't read mappings", e);
        }
    }

    @Override
    public String mapClassName(String original, boolean backwards) {
        if (backwards) return classNames.inverse().get(original);
        return classNames.getOrDefault(original, original);
    }

    @Override
    public String mapFieldName(String owner, String original, String desc, boolean backwards) {
        if (backwards) return fieldNames.getOrDefault(owner, HashBiMap.create()).inverse().get(original);
        return fieldNames.getOrDefault(owner, HashBiMap.create()).getOrDefault(original, original);
    }

    @Override
    public String mapMethodName(String owner, String original, String desc, boolean backwards) {
        if (backwards) return methodNames.getOrDefault(owner, YarnMappingProvider.EMPTY).inverse().get(new Pair<>(original, desc)).getFirst();
        return methodNames.getOrDefault(owner, YarnMappingProvider.EMPTY).getOrDefault(new Pair<>(original, desc), new Pair<>(original, desc)).getFirst();
    }

    private static JsonObject getVersions() {
        AtomicReference<JsonObject> objectAtomic = new AtomicReference<>();

        try {
            PufferfishGradle.useCachedHttpResource(new URL(Constants.MCP_VERSIONS_URL), "mcpVersions.json", "Couldn't fetch MCP versions", stream -> {
                try (InputStreamReader reader = new InputStreamReader(stream)) {
                    objectAtomic.set(new JsonParser().parse(reader).getAsJsonObject());
                }
            });
        } catch (MalformedURLException e) {
            throw new GradleException("Impossible", e);
        }
        return objectAtomic.get();
    }

    private static Pair<String, Integer> getLatest(String version) {
        JsonObject obj = getVersions().getAsJsonObject(version);
        if (obj == null) return null;
        if (obj.has("stable") && obj.getAsJsonArray("stable").size() > 0) {
            return new Pair<>("stable", obj.getAsJsonArray("stable").get(0).getAsInt());
        } else if (obj.has("snapshot")) {
            return new Pair<>("snapshot", obj.getAsJsonArray("snapshot").get(0).getAsInt());
        } else {
            return null;
        }
    }
}
