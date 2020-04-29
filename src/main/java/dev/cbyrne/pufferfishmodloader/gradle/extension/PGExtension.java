package dev.cbyrne.pufferfishmodloader.gradle.extension;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import java.util.HashSet;
import java.util.Set;

public class PGExtension {
    private final NamedDomainObjectContainer<ModExtension> modContainer;
    private SourceSet mainSourceSet;
    private final Set<TargetExtension> targetVersions = new HashSet<>();
    private final PufferfishGradle plugin; // Public so people can still access things in project from our extension

    public PGExtension(PufferfishGradle plugin) {
        this.plugin = plugin;
        modContainer = plugin.getProject().container(ModExtension.class, new ModExtensionFactory(plugin.getProject()));
        mainSourceSet = plugin.getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main");
    }

    public void target(String... versions) {
        for (String version : versions) {
            targetVersions.add(new TargetExtension(version, plugin));
        }
    }

    public void target(String version, Closure<?> closure) {
        TargetExtension target = new TargetExtension(version, plugin);
        plugin.getProject().configure(target, closure);
        targetVersions.add(target);
    }

    public Set<TargetExtension> getTargetVersions() {
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
