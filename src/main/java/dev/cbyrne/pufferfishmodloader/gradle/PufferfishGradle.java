package dev.cbyrne.pufferfishmodloader.gradle;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cbyrne.pufferfishmodloader.gradle.extension.PGExtension;
import dev.cbyrne.pufferfishmodloader.gradle.extension.TargetExtension;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.TaskDownloadJar;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.TaskMergeJars;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.mods.TaskGenerateModJson;
import dev.cbyrne.pufferfishmodloader.gradle.utils.HttpUtils;
import dev.cbyrne.pufferfishmodloader.gradle.utils.InputStreamConsumer;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.Artifact;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.VersionJson;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.typeadapters.ArgumentTypeAdapter;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest.VersionManifest;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest.VersionManifestEntry;
import org.apache.commons.io.IOUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.function.Consumer;

import static dev.cbyrne.pufferfishmodloader.gradle.utils.Constants.*;

public class PufferfishGradle implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(ArgumentTypeAdapter.FACTORY)
            .create();
    private final Map<String, VersionJson> manifests = Maps.newHashMap();
    private final Map<String, byte[]> httpResourceCache = Maps.newHashMap();
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
        project.getExtensions().add(EXTENSION_NAME, extension = new PGExtension(this));
        libraryConfiguration = project.getConfigurations().create(LIBRARY_CONFIGURATION_NAME);
        project.getConfigurations().create(MAPPINGS_CONFIGURATION_NAME);
        project.getConfigurations().create(INTERMEDIARY_CONFIGURATION_NAME);
        Configuration embedConfig = project.getConfigurations().create(EMBED_CONFIGURATION_NAME); // For file dependencies

        project.afterEvaluate(p -> {
            p.getRepositories().mavenCentral();
            p.getRepositories().maven(mavenArtifactRepository -> mavenArtifactRepository.setUrl("https://libraries.minecraft.net"));
            p.getRepositories().maven(mavenArtifactRepository -> mavenArtifactRepository.setUrl(new File(cacheDir, "repo").toURI()));

            for (TargetExtension version : extension.getTargetVersions()) {
                setupVersion(version);
            }

            setupModsJson();

            setupEmbed(embedConfig);
        });
    }

    private void setupVersion(TargetExtension versionObj) {
        versionObj.getMappings().load(this, versionObj.getVersion());
        String version = versionObj.getVersion();
        // Set up source set
        JavaPluginConvention javaPlugin = getJavaPlugin();
        SourceSet sourceSet = javaPlugin.getSourceSets().maybeCreate("mc" + version);
        /*project.getDependencies().add(sourceSet.getCompileConfigurationName(), ImmutableMap.of(
                "group", MINECRAFT_GROUP,
                "name", MINECRAFT_ARTIFACT,
                "version", version
        ));*/ // TODO: Uncomment when this artifact can actually be generated
        project.getDependencies().add(sourceSet.getCompileConfigurationName(), extension.getMainSourceSet().getRuntimeClasspath());

        runWithJarTask(jar -> {
            jar.dependsOn(sourceSet.getClassesTaskName());
            jar.from(sourceSet.getOutput());
        });

        VersionJson json = getVersionJson(version);
        TaskDownloadJar clientJar = setupJarDownload(json, json.getDownloads().getClient(), TASK_DOWNLOAD_CLIENT, "client");
        TaskDownloadJar serverJar = setupJarDownload(json, json.getDownloads().getServer(), TASK_DOWNLOAD_SERVER, "server");

        TaskMergeJars mergeJars = project.getTasks().create(TASK_MERGE_JARS + version, TaskMergeJars.class);
        mergeJars.setClientJar(clientJar.getDest());
        mergeJars.setServerJar(serverJar.getDest());
        mergeJars.setOutputJar(new File(cacheDir, "versions/" + version + "/merged.jar"));
        mergeJars.dependsOn(clientJar.getName(), serverJar.getName());
    }

    @SuppressWarnings("UnstableApiUsage")
    private void runWithJarTask(Consumer<Jar> consumer) {
        for (Task task : project.getTasks()) {
            if (task.getName().equalsIgnoreCase(extension.getMainSourceSet().getJarTaskName())) {
                consumer.accept((Jar) task);
                break;
            }
        }
    }

    private TaskDownloadJar setupJarDownload(VersionJson version, Artifact artifact, String taskBaseName, String type) {
        TaskDownloadJar task = project.getTasks().create(taskBaseName + version.getId(), TaskDownloadJar.class);
        task.setUrl(artifact.getUrl());
        task.setDest(new File(cacheDir, "versions/" + version.getId() + "/" + type + ".jar"));
        task.setSha1(artifact.getSha1());
        return task;
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

    public void useCachedHttpResource(URL url, String filename, String errorMessage, InputStreamConsumer consumer) {
        if (!httpResourceCache.containsKey(filename)) {
            File cachedFile = new File(cacheDir, filename);
            try {
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("User-Agent", USER_AGENT);
                cachedFile.getParentFile().mkdirs();
                try (InputStream stream = url.openStream(); FileOutputStream writer = new FileOutputStream(cachedFile)) {
                    byte[] bytes = IOUtils.toByteArray(stream);
                    httpResourceCache.put(filename, bytes);
                    writer.write(bytes);
                } catch (IOException e) {
                    throw new GradleException(errorMessage, e);
                }
            } catch (IOException e) {
                if (cachedFile.exists()) {
                    try (InputStream stream = new FileInputStream(cachedFile)) {
                        byte[] bytes = IOUtils.toByteArray(stream);
                        httpResourceCache.put(filename, bytes);
                    } catch (IOException ex) {
                        throw new GradleException(errorMessage, ex);
                    }
                } else {
                    throw new GradleException(errorMessage, e);
                }
            }
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(httpResourceCache.get(filename))) {
            consumer.accept(input);
        } catch (IOException e) {
            throw new GradleException(errorMessage, e);
        }
    }

    private VersionManifest getVersionManifest() {
        if (versionManifest == null) {
            try {
                useCachedHttpResource(new URL(VERSION_MANIFEST_URL), "versionManifest.json", "Couldn't fetch version manifest", stream -> {
                    try (InputStreamReader reader = new InputStreamReader(stream)) {
                        versionManifest = GSON.fromJson(reader, VersionManifest.class);
                    }
                });
            } catch (MalformedURLException e) {
                throw new GradleException("Impossible", e);
            }
        }
        return versionManifest;
    }

    private JavaPluginConvention getJavaPlugin() {
        return project.getConvention().getPlugin(JavaPluginConvention.class);
    }

    private void setupEmbed(Configuration embedConfig) {
        runWithJarTask(jarTask -> embedConfig.forEach(file -> jarTask.from(file.isDirectory() ? file : project.zipTree(file))));
    }

    @SuppressWarnings("UnstableApiUsage")
    private void setupModsJson() {
        if (extension.getModContainer().isEmpty()) return;
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

    public File getCacheDir() {
        return cacheDir;
    }
}
