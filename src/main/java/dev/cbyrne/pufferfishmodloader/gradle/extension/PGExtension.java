package dev.cbyrne.pufferfishmodloader.gradle.extension;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

public class PGExtension {
    private final NamedDomainObjectContainer<ModExtension> modContainer;

    public PGExtension(Project project) {
        modContainer = project.container(ModExtension.class, new ModExtensionFactory(project));
    }

    public void mods(Closure<?> closure) {
        modContainer.configure(closure);
    }

    public NamedDomainObjectContainer<ModExtension> getModContainer() {
        return modContainer;
    }
}
