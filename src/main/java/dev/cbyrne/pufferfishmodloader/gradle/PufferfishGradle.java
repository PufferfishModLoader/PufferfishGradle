package dev.cbyrne.pufferfishmodloader.gradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cbyrne.pufferfishmodloader.gradle.extension.PGExtension;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.mods.TaskGenerateModJson;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.jvm.tasks.Jar;

import javax.annotation.Nonnull;

public class PufferfishGradle implements Plugin<Project> {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
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
        project.getExtensions().add("minecraft", extension = new PGExtension(project));
        libraryConfiguration = project.getConfigurations().create("library");
        Configuration embedConfig = project.getConfigurations().create("embed"); // For file dependencies

        project.afterEvaluate(p -> {
            TaskGenerateModJson task = p.getTasks().create("genModJson", TaskGenerateModJson.class);
            task.setLibraryConfiguration(libraryConfiguration);
            task.setModContainer(extension.getModContainer());
            task.setOutput(project.file("mods.json"));

            Jar jarTask = (Jar) p.getTasks().getByName("jar");
            embedConfig.forEach(file -> jarTask.from(file.isDirectory() ? file : p.zipTree(file)));
        });
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
