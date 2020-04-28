package dev.cbyrne.pufferfishmodloader.gradle.extension;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PGExtension {
    private final NamedDomainObjectContainer<ModExtension> modContainer;
    private SourceSet mainSourceSet;
    private final List<String> targetVersions = new ArrayList<>();

    public PGExtension(Project project) {
        modContainer = project.container(ModExtension.class, new ModExtensionFactory(project));
        mainSourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main");
    }

    public void targets(String... versions) {
        targetVersions.addAll(Arrays.asList(versions));
    }

    public List<String> getTargetVersions() {
        return targetVersions;
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
