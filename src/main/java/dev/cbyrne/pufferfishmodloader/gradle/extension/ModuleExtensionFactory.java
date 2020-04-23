package dev.cbyrne.pufferfishmodloader.gradle.extension;

import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

public class ModuleExtensionFactory implements NamedDomainObjectFactory<ModuleExtension> {
    private final Project project;

    public ModuleExtensionFactory(Project project) {
        this.project = project;
    }

    @Override
    public @Nonnull ModuleExtension create(@Nonnull String s) {
        return new ModuleExtension(s, project.getVersion().toString());
    }
}
