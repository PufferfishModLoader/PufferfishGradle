package dev.cbyrne.pufferfishmodloader.gradle.extension;

import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

public class ModExtensionFactory implements NamedDomainObjectFactory<ModExtension> {
    private final Project project;

    public ModExtensionFactory(Project project) {
        this.project = project;
    }

    @Override
    public @Nonnull
    ModExtension create(@Nonnull String s) {
        project.getConfigurations().create(getConfigurationName(s));
        return new ModExtension(s, project.getVersion().toString());
    }

    public static String getConfigurationName(String id) {
        return id + "Library";
    }
}
