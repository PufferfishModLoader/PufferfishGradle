package dev.cbyrne.pufferfishmodloader.gradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cbyrne.pufferfishmodloader.gradle.extension.PGExtension;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.mods.TaskGenerateModJson;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.typeadapters.ArgumentTypeAdapter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.annotation.Nonnull;
import java.io.File;

import static dev.cbyrne.pufferfishmodloader.gradle.utils.Constants.*;

public class PufferfishGradle implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(ArgumentTypeAdapter.FACTORY)
            .create();
    private Project project;
    private Configuration libraryConfiguration;
    private PGExtension extension;

    @Override
    public void apply(@Nonnull Project project) {
        if (!project.getPluginManager().hasPlugin("java")) {
            project.getPluginManager().apply("java");
        }

        this.project = project;
        project.getExtensions().add(EXTENSION_NAME, extension = new PGExtension(project));
        libraryConfiguration = project.getConfigurations().create(LIBRARY_CONFIGURATION_NAME);
        Configuration embedConfig = project.getConfigurations().create(EMBED_CONFIGURATION_NAME); // For file dependencies

        project.afterEvaluate(p -> {
            setupModsJson();

            setupEmbed(embedConfig);
        });
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
