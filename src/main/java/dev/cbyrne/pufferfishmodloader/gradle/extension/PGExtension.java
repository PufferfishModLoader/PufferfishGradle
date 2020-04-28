package dev.cbyrne.pufferfishmodloader.gradle.extension;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

public class PGExtension {
    private final NamedDomainObjectContainer<ModExtension> modContainer;
    private SourceSet mainSourceSet;

    public PGExtension(Project project) {
        modContainer = project.container(ModExtension.class, new ModExtensionFactory(project));
        mainSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main");
    }

    public void sourceSet(SourceSet set) {
        mainSourceSet = set;
    }

    public SourceSet getMainSourceSet() {
        return mainSourceSet;
    }

    public void mods(Closure<?> closure) {
        modContainer.configure(closure);
    }

    public NamedDomainObjectContainer<ModExtension> getModContainer() {
        return modContainer;
    }
}
