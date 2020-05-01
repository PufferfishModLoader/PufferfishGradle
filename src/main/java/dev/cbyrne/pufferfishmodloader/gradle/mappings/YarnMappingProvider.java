package dev.cbyrne.pufferfishmodloader.gradle.mappings;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.utils.Constants;
import net.md_5.specialsource.JarMapping;
import org.apache.commons.io.IOUtils;
import org.gradle.api.GradleException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class YarnMappingProvider extends MappingProvider implements Serializable {
    private String version;

    @Override
    public void initialize(PufferfishGradle plugin, String mcVersion) {
        useLatestFrom(mcVersion);
    }

    public void useLatestFrom(String mcVersion) {
        if (isAvailable(mcVersion)) {
            version = getVersions(mcVersion).get(0).getAsJsonObject()
                    .getAsJsonObject("mappings")
                    .get("version").getAsString();
        } else {
            version = null;
        }
    }

    public void version(String version) {
        this.version = version;
    }

    public void setVersion(String version) {
        version(version);
    }

    @Override
    public void load(PufferfishGradle plugin, String version, JarMapping dest) {
        plugin.getProject().getRepositories().maven(maven -> maven.setUrl("https://maven.fabricmc.net"));
        plugin.getProject().getConfigurations().create(Constants.MAPPINGS_CONFIGURATION_NAME + version);
        plugin.getProject().getDependencies().add(Constants.MAPPINGS_CONFIGURATION_NAME + version, ImmutableMap.of(
                "group", "net.fabricmc",
                "name", "yarn",
                "version", this.version
        ));
        for (File f : plugin.getProject().getConfigurations().getByName(Constants.MAPPINGS_CONFIGURATION_NAME + version)) {
            if (f.getName().endsWith(".jar")) {
                // this is the mapping jar

                try (ZipFile file = new ZipFile(f)) {
                    ZipEntry entry = file.getEntry("mappings/mappings.tiny");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(entry)))) {
                        // Class Format: <type: CLASS> <official> <intermediary> <named>
                        // Method/field Format: <type> <owner> <official desc> <official name> <intermediary> <named>
                        List<String[]> nonClassLines = new ArrayList<>(); // We store them here for later to be able to remap descs

                        {
                            @SuppressWarnings("UnusedAssignment") String line = reader.readLine(); // discard first line
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split("[ \t]+");
                                if (parts[0].equalsIgnoreCase("CLASS")) {
                                    dest.classes.put(parts[1], parts[3]);
                                } else {
                                    nonClassLines.add(parts);
                                }
                            }
                        }

                        for (String[] line : nonClassLines) {
                            String owner = line[1];
                            String officialDesc = line[2];
                            String officialName = line[3];
                            String mappedName = line[5];
                            switch (line[0]) {
                                case "FIELD":
                                    dest.fields.put(owner + '/' + officialName, mappedName);
                                    break;
                                case "METHOD":
                                    dest.methods.put(owner + '/' + officialName + ' ' + officialDesc, mappedName);
                                    break;
                                default:
                                    throw new GradleException("Unknown type " + line[0]);
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new GradleException("Failed to load Yarn mappings", e);
                }
                break;
            }
        }
    }

    @Override
    public void checkParamsCorrect(PufferfishGradle plugin, String version) {
        try {
            PufferfishGradle.useCachedHttpResource(new URL(Constants.YARN_MAVEN_METADATA_URL), "yarnMavenMetadata.xml", "Couldn't fetch yarn maven metadata", stream -> {
                String s = IOUtils.toString(stream, StandardCharsets.UTF_8);
                if (!s.contains("<version>" + YarnMappingProvider.this.version + "</version>")) { // yes, i know, this is very bad xml parsing. leave me alone.
                    throw new GradleException("Invalid Yarn version");
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private static JsonArray getVersions(String version) {
        AtomicReference<JsonArray> atomicArray = new AtomicReference<>();
        try {
            PufferfishGradle.useCachedHttpResource(new URL(Constants.YARN_VERSIONS_URL + version), "yarnVersions" + version + ".json", "Couldn't fetch yarn versions", stream -> {
                try (InputStreamReader reader = new InputStreamReader(stream)) {
                    atomicArray.set(new JsonParser().parse(reader).getAsJsonArray());
                }
            });
        } catch (MalformedURLException e) {
            throw new GradleException("Impossible", e);
        }
        return atomicArray.get();
    }

    public static boolean isAvailable(String version) {
        return getVersions(version).size() > 0;
    }
}
