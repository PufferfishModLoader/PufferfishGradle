package dev.cbyrne.pufferfishmodloader.gradle;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cbyrne.pufferfishmodloader.gradle.extension.PGExtension;
import dev.cbyrne.pufferfishmodloader.gradle.extension.TargetExtension;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.remap.TaskDeobfJar;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.TaskDownloadJar;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.TaskMergeJars;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.mods.TaskGenerateModJson;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.workspace.TaskDownloadAssets;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.workspace.TaskGenRunConfigs;
import dev.cbyrne.pufferfishmodloader.gradle.utils.HttpUtils;
import dev.cbyrne.pufferfishmodloader.gradle.utils.InputStreamConsumer;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.*;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.Rule;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.typeadapters.ArgumentTypeAdapter;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest.VersionManifest;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.manifest.VersionManifestEntry;
import org.apache.commons.io.IOUtils;
import org.gradle.api.*;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static dev.cbyrne.pufferfishmodloader.gradle.utils.Constants.*;

@SuppressWarnings("UnstableApiUsage")
public class PufferfishGradle implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(ArgumentTypeAdapter.FACTORY)
            .create();
    private final Map<String, VersionJson> manifests = Maps.newHashMap();
    private static final Map<String, byte[]> httpResourceCache = Maps.newHashMap();
    private VersionManifest versionManifest;
    private static File cacheDir;
    private Project project;
    private Configuration libraryConfiguration;
    private PGExtension extension;

    @Override
    public void apply(@Nonnull Project project) {
        if (!project.getPluginManager().hasPlugin("java")) {
            project.getPluginManager().apply("java");
        }
        if (!project.getPluginManager().hasPlugin("idea")) {
            project.getPluginManager().apply("idea");
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
            getJavaPlugin().setSourceCompatibility(JavaVersion.VERSION_1_8);
            getJavaPlugin().setTargetCompatibility(JavaVersion.VERSION_1_8);

            p.getRepositories().mavenCentral();
            p.getRepositories().maven(mavenArtifactRepository -> mavenArtifactRepository.setUrl("https://libraries.minecraft.net"))
                    .metadataSources(MavenArtifactRepository.MetadataSources::artifact);
            p.getRepositories().maven(mavenArtifactRepository -> mavenArtifactRepository.setUrl(new File(cacheDir, "repo").toURI()))
                    .metadataSources(MavenArtifactRepository.MetadataSources::artifact);

            List<String> taskDeps = new ArrayList<>();
            for (TargetExtension version : extension.getTargetVersions()) {
                taskDeps.add(setupVersion(version));
            }
            if (extension.getTargetVersions().isEmpty()) {
                // TODO: Fetch latest supported version from PML servers
                taskDeps.add(setupVersion(new TargetExtension(
                        "1.15.2",
                        this
                )));
            }

            p.getTasks().register("setup", task -> {
                task.setGroup("pufferfishgradle");
                task.dependsOn(taskDeps.toArray(new Object[0]));
            });

            setupModsJson();

            setupEmbed(embedConfig);
        });
    }

    private String setupVersion(TargetExtension versionObj) {
        versionObj.getMappings().checkParamsCorrect(this, versionObj.getVersion());
        String version = versionObj.getVersion();
        // Set up source set
        JavaPluginConvention javaPlugin = getJavaPlugin();
        SourceSet sourceSet = javaPlugin.getSourceSets().maybeCreate("mc" + version);
        project.getDependencies().add(sourceSet.getImplementationConfigurationName(), ImmutableMap.of(
                "group", MINECRAFT_GROUP,
                "name", MINECRAFT_ARTIFACT,
                "version", version
        ));
        project.getDependencies().add(sourceSet.getImplementationConfigurationName(), extension.getMainSourceSet().getRuntimeClasspath());

        VersionJson json = getVersionJson(version);
        for (Library library : json.getLibraries()) {
            if (!library.getName().equals("tv.twitch:twitch-external-platform:4.5") && Rule.allow(library.getRules(), Maps.newHashMap())) {
                if (library.getDownloads() != null && library.getDownloads().getArtifact() != null) {
                    String path = library.getDownloads().getArtifact().getPath();
                    if (path == null) {
                        String[] parts = library.getName().split(":");
                        path = parts[0].replace('.', '/') + '/' + parts[1] + '/' + parts[2] + '/' + parts[1] + '-' + parts[2] + ".jar";
                    }
                    File f = new File(cacheDir, "libraries/" + path);
                    try {
                        HttpUtils.download(library.getDownloads().getArtifact().getUrl(), f, library.getDownloads().getArtifact().getSha1(), 5);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    project.getDependencies().add(sourceSet.getImplementationConfigurationName(), project.files(f));
                } else {
                    project.getDependencies().add(sourceSet.getImplementationConfigurationName(), library.getName());
                }
                if (library.getNatives() != null && library.getNatives().containsKey(OperatingSystem.current())) {
                    LibraryArtifact artifact = library.getDownloads().getClassifiers().get(library.getNatives().get(OperatingSystem.current()));
                    String path = artifact.getPath();
                    if (path == null) {
                        String[] parts = library.getName().split(":");
                        path = parts[0].replace('.', '/') + '/' + parts[1] + '/' + parts[2] + '/' + parts[1] + '-' + parts[2] + '-' + library.getNatives().get(OperatingSystem.current()) + ".jar";
                    }
                    File f = new File(cacheDir, "libraries/" + path);
                    try {
                        HttpUtils.download(artifact.getUrl(), f, artifact.getSha1(), 5);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    project.getDependencies().add(sourceSet.getImplementationConfigurationName(), project.files(f));
                }
            }
        }

        if (extension.isSeparateVersionJars()) {
            project.getTasks().register(sourceSet.getJarTaskName(), Jar.class, task -> {
                task.dependsOn(sourceSet.getClassesTaskName(), extension.getMainSourceSet().getClassesTaskName());
                task.from(sourceSet.getOutput());
                task.from(extension.getMainSourceSet().getOutput());
                task.getArchiveClassifier().set(version);
                task.setGroup("build");
            });

            project.getTasks().getByName("assemble").dependsOn(sourceSet.getJarTaskName());
        } else {
            runWithJarTask(jar -> {
                jar.dependsOn(sourceSet.getClassesTaskName());
                jar.from(sourceSet.getOutput());
            });
        }

        // Set up tasks
        Provider<TaskDownloadJar> clientJar = setupJarDownload(json, json.getDownloads().getClient(), TASK_DOWNLOAD_CLIENT, "client");
        Provider<TaskDownloadJar> serverJar = setupJarDownload(json, json.getDownloads().getServer(), TASK_DOWNLOAD_SERVER, "server");

        Provider<TaskMergeJars> mergeJars = project.getTasks().register(TASK_MERGE_JARS + version, TaskMergeJars.class, task -> {
            task.setClientJar(clientJar.get().getDest());
            task.setServerJar(serverJar.get().getDest());
            task.setOutputJar(new File(cacheDir, "versions/" + version + "/merged.jar"));
            task.dependsOn(clientJar.get().getName(), serverJar.get().getName());
        });

        project.getTasks().register(TASK_DEOBF_JAR + version, TaskDeobfJar.class, task -> {
            task.setInput(mergeJars.get().getOutputJar());
            task.setMappings(versionObj.getMappings());
            task.setOutput(new File(cacheDir, "repo/net/minecraft/minecraft/" + version + "/minecraft-" + version + ".jar"));
            task.dependsOn(mergeJars.get().getName());
            task.setBackwards(false);
            task.setPlugin(this);
            task.setVersion(version);
            task.setAccessTransformers(versionObj.getAccessTransformers());
        });

        project.getTasks().register(TASK_DOWNLOAD_ASSETS + version, TaskDownloadAssets.class, task -> {
            task.setPlugin(this);
            task.setVersion(json);
        });

        setupRunConfigTasks(versionObj.getRunDir(), json, sourceSet, versionObj.getClientMainClass(), versionObj.getServerMainClass());
        return "genRunConfigs" + version;
    }

    private void setupRunConfigTasks(File workDirBase, VersionJson version, SourceSet sourceSet, String cmc, String smc) {
        Provider<TaskGenRunConfigs> t1 = setupRunConfigTask(new File(workDirBase, "client"), version, true, sourceSet, cmc);
        Provider<TaskGenRunConfigs> t2 = setupRunConfigTask(new File(workDirBase, "server"), version, false, sourceSet, smc);
        project.getTasks().register("genRunConfigs" + version.getId(), task -> task.dependsOn(t1.get().getName(), t2.get().getName(), TASK_DEOBF_JAR + version.getId(), TASK_DOWNLOAD_ASSETS + version.getId()));
    }

    private Provider<TaskGenRunConfigs> setupRunConfigTask(File workDirectory, VersionJson version, boolean client, SourceSet sourceSet, String mainClass) {
        String name = "genRunConfig" + version.getId();
        if (client) {
            name += "Client";
        } else {
            name += "Server";
        }
        return project.getTasks().register(name, TaskGenRunConfigs.class, task -> {
            String configName = "Minecraft " + version.getId() + ' ';
            if (client) {
                configName += "Client";
            } else {
                configName += "Server";
            }
            task.setSourceSetName(sourceSet.getName());
            task.setConfigName(configName);
            task.setOptions(new ArrayList<>());
            task.setWorkingDirectory(workDirectory);
            task.setVmOptions(new ArrayList<>());
            if (version.getArguments() != null && version.getArguments().getJvm() != null && OperatingSystem.current() == OperatingSystem.MACOS) {
                task.getVmOptions().add("-XstartOnFirstThread");
            }
            task.setEnvironmentVars(new HashMap<>());
            task.getEnvironmentVars().put("MAIN_CLASS", mainClass);
            task.getEnvironmentVars().put("ASSET_DIRECTORY", new File(cacheDir, "assets").getAbsolutePath());
            task.getEnvironmentVars().put("ASSET_INDEX", version.getAssetIndex().getId());
            task.getEnvironmentVars().put("RUN_DIRECTORY", workDirectory.getAbsolutePath());
        });
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

    private Provider<TaskDownloadJar> setupJarDownload(VersionJson version, Artifact artifact, String taskBaseName, String type) {
        return project.getTasks().register(taskBaseName + version.getId(), TaskDownloadJar.class, task -> {
            task.setUrl(artifact.getUrl());
            task.setDest(new File(cacheDir, "versions/" + version.getId() + "/" + type + ".jar"));
            task.setSha1(artifact.getSha1());
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

    public static void useCachedHttpResource(URL url, String filename, String errorMessage, InputStreamConsumer consumer) {
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
        ProcessResources processResourcesTask = (ProcessResources) project.getTasks().getByName(extension.getMainSourceSet().getProcessResourcesTaskName());
        project.getTasks().register(TASK_GEN_MODS_JSON, TaskGenerateModJson.class, task -> {
            task.setLibraryConfiguration(libraryConfiguration);
            task.setModContainer(extension.getModContainer());
            task.setOutput(new File(task.getTemporaryDir(), "mods.json"));
            processResourcesTask.from(task.getOutput());
        });
        processResourcesTask.dependsOn(TASK_GEN_MODS_JSON);
    }

    public Project getProject() {
        return project;
    }

    public File getCacheDir() {
        return cacheDir;
    }
}
