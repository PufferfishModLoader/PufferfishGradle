package dev.cbyrne.pufferfishmodloader.gradle;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cbyrne.pufferfishmodloader.gradle.extension.PGExtension;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.mods.TaskGenerateModJson;
import dev.cbyrne.pufferfishmodloader.gradle.utils.Constants;
import dev.cbyrne.pufferfishmodloader.gradle.utils.HashUtils;
import dev.cbyrne.pufferfishmodloader.gradle.utils.HttpUtils;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.VersionJson;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.typeadapters.ArgumentTypeAdapter;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest.VersionManifest;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest.VersionManifestEntry;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URL;
import java.util.Map;

import static dev.cbyrne.pufferfishmodloader.gradle.utils.Constants.*;

public class PufferfishGradle implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(ArgumentTypeAdapter.FACTORY)
            .create();
    private final Map<String, VersionJson> manifests = Maps.newHashMap();
    private VersionManifest versionManifest;
    private File cacheDir;
    private Project project;
    private Configuration libraryConfiguration;
    private PGExtension extension;

    @Override
    public void apply(@Nonnull Project project) {
        if (!project.getPluginManager().hasPlugin("java")) {
            project.getPluginManager().apply("java");
        }
        this.project = project;
        cacheDir = new File(project.getGradle().getGradleUserHomeDir(), "caches/pufferfishgradle");
        cacheDir.mkdirs();
        project.getExtensions().add(EXTENSION_NAME, extension = new PGExtension(project));
        libraryConfiguration = project.getConfigurations().create(LIBRARY_CONFIGURATION_NAME);
        Configuration embedConfig = project.getConfigurations().create(EMBED_CONFIGURATION_NAME); // For file dependencies

        project.afterEvaluate(p -> {
            setupModsJson();

            setupEmbed(embedConfig);
        });
    }

    public VersionJson getVersionJson(String version) {
        return manifests.computeIfAbsent(version, v -> {
            File versionJsonFile = new File(cacheDir, "versions/" + v + "/version.json");
            if (!versionJsonFile.exists()) {
                for (VersionManifestEntry entry : getVersionManifest().getVersions()) {
                    if (entry.getId().equals(v)) {
                        versionJsonFile.getParentFile().mkdirs();
                        try {
                            HttpUtils.download(entry.getUrl(), versionJsonFile, null, 1);
                        } catch (IOException e) {
                            throw new GradleException("Couldn't download version json", e);
                        }
                    }
                }
            }
            try (FileReader reader = new FileReader(versionJsonFile)) {
                return GSON.fromJson(reader, VersionJson.class);
            } catch (IOException e) {
                throw new GradleException("Can't read version json file", e);
            }
        });
    }

    private VersionManifest getVersionManifest() {
        if (versionManifest == null) {
            File versionManifestFile = new File(cacheDir, "versionManifest.json");
            try (InputStreamReader reader = new InputStreamReader(new URL(VERSION_MANIFEST_URL).openStream())) {
                versionManifest = GSON.fromJson(reader, VersionManifest.class);
            } catch (Exception e) {
                if (versionManifestFile.exists()) {
                    try (FileReader reader = new FileReader(versionManifestFile)) {
                        versionManifest = GSON.fromJson(reader, VersionManifest.class);
                    } catch (IOException ex) {
                        throw new GradleException("Can't fetch version manifest", e);
                    }
                } else {
                    throw new GradleException("Can't fetch version manifest", e);
                }
            }
        }
        return versionManifest;
    }

    private JavaPluginConvention getJavaPlugin() {
        return project.getConvention().getPlugin(JavaPluginConvention.class);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void setupEmbed(Configuration embedConfig) {
        Jar jarTask = (Jar) project.getTasks().getByName(extension.getMainSourceSet().getJarTaskName());
        embedConfig.forEach(file -> jarTask.from(file.isDirectory() ? file : project.zipTree(file)));
    }

    @SuppressWarnings("UnstableApiUsage")
    private void setupModsJson() {
        TaskGenerateModJson modJsonTask = project.getTasks().create(TASK_GEN_MODS_JSON, TaskGenerateModJson.class);
        modJsonTask.setLibraryConfiguration(libraryConfiguration);
        modJsonTask.setModContainer(extension.getModContainer());
        modJsonTask.setOutput(new File(modJsonTask.getTemporaryDir(), "mods.json"));

        ProcessResources processResourcesTask = (ProcessResources) project.getTasks().getByName(extension.getMainSourceSet().getProcessResourcesTaskName());
        processResourcesTask.from(modJsonTask.getOutput());
        processResourcesTask.dependsOn(TASK_GEN_MODS_JSON);
    }

    public Project getProject() {
        return project;
    }

    public Configuration getLibraryConfiguration() {
        return libraryConfiguration;
    }

    public PGExtension getExtension() {
        return extension;
    }
}
