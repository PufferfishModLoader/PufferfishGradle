package dev.cbyrne.pufferfishmodloader.gradle.extension;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

public class PGExtension {
    private final NamedDomainObjectContainer<ModuleExtension> moduleContainer;

    public PGExtension(Project project) {
        moduleContainer = project.container(ModuleExtension.class, new ModuleExtensionFactory(project));
    }

    public void modules(Closure<?> closure) {
        moduleContainer.configure(closure);
    }

    public NamedDomainObjectContainer<ModuleExtension> getModuleContainer() {
        return moduleContainer;
    }
}
