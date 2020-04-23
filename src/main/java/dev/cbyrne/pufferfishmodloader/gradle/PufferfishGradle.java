package dev.cbyrne.pufferfishmodloader.gradle;

import dev.cbyrne.pufferfishmodloader.gradle.extension.PGExtension;
import dev.cbyrne.pufferfishmodloader.gradle.tasks.modules.TaskGenerateModuleJson;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import javax.annotation.Nonnull;

public class PufferfishGradle implements Plugin<Project> {
    private Project project;
    private Configuration libraryConfiguration;
    private PGExtension extension;

    @Override
    public void apply(@Nonnull Project project) {
        this.project = project;
        project.getExtensions().add("minecraft", extension = new PGExtension(project));
        libraryConfiguration = project.getConfigurations().create("library");

        project.afterEvaluate(p -> {
            TaskGenerateModuleJson task = p.getTasks().create("genModuleJson", TaskGenerateModuleJson.class);
            task.setLibraryConfiguration(libraryConfiguration);
            task.setModuleContainer(extension.getModuleContainer());
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
