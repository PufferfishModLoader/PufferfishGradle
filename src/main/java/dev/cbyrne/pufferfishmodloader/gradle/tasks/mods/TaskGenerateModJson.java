package dev.cbyrne.pufferfishmodloader.gradle.tasks.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.extension.ModExtension;
import dev.cbyrne.pufferfishmodloader.gradle.extension.ModExtensionFactory;
import dev.cbyrne.pufferfishmodloader.gradle.utils.HashUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TaskGenerateModJson extends DefaultTask {
    @InputFiles
    private Configuration libraryConfiguration;
    @Input
    private NamedDomainObjectContainer<ModExtension> modContainer;
    @OutputFile
    private File output;

    @TaskAction
    public void generate() {
        JsonArray mods = new JsonArray();

        for (ModExtension mod : modContainer) {
            JsonObject modObj = new JsonObject();

            modObj.addProperty("id", mod.getName());
            modObj.addProperty("version", mod.getVersion());
            modObj.add("dependencies", listToJsonArray(mod.getDependencies(), JsonPrimitive::new));
            modObj.add("optional_dependencies", listToJsonArray(mod.getOptionalDependencies(), JsonPrimitive::new));
            modObj.add("load_before", listToJsonArray(mod.getLoadBefore(), JsonPrimitive::new));
            modObj.add("incompatibilities", listToJsonArray(mod.getIncompatibilities(), JsonPrimitive::new));
            List<JsonObject> libraries = getLibrariesFromConfiguration(libraryConfiguration);
            libraries.addAll(getLibrariesFromConfiguration(getProject().getConfigurations().getByName(ModExtensionFactory.getConfigurationName(mod.getName()))));
            modObj.add("libraries", listToJsonArray(libraries, obj -> obj));

            mods.add(modObj);
        }

        try (FileWriter writer = new FileWriter(getOutput())) {
            PufferfishGradle.GSON.toJson(mods, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write mods.json", e);
        }
    }

    private List<JsonObject> getLibrariesFromConfiguration(Configuration config) {
        List<JsonObject> rv = new ArrayList<>();

        config.getResolvedConfiguration().getResolvedArtifacts().forEach(artifact -> {
            ComponentArtifactIdentifier artifactId = artifact.getId();
            if (artifactId instanceof ModuleComponentArtifactIdentifier) {
                // Build URL path from component data
                ModuleComponentArtifactIdentifier id = (ModuleComponentArtifactIdentifier) artifactId;
                String urlPath = id.getComponentIdentifier().getGroup().replace('.', '/') + '/'
                        + id.getComponentIdentifier().getModule() + '/'
                        + id.getComponentIdentifier().getVersion() + '/';
                ModuleComponentIdentifier cid = id.getComponentIdentifier();
                if (cid instanceof MavenUniqueSnapshotComponentIdentifier) {
                    MavenUniqueSnapshotComponentIdentifier muscid = (MavenUniqueSnapshotComponentIdentifier) cid;
                    urlPath += muscid.getModule() + '-'
                            + muscid.getVersion().replace("-SNAPSHOT", "") + '-'
                            + muscid.getTimestamp();
                } else {
                    urlPath += cid.getModule() + '-'
                            + cid.getVersion().replace("-SNAPSHOT", "");
                }
                if (artifact.getClassifier() != null) {
                    urlPath += '-' + artifact.getClassifier();
                }
                urlPath += '.' + artifact.getExtension();
                // Merge URL path with the repository the artifact is in
                URL url = null;
                for (ArtifactRepository repo : getProject().getRepositories()) {
                    if (repo instanceof MavenArtifactRepository) {
                        try {
                            URL artifactUrl = new URL(((MavenArtifactRepository) repo).getUrl().toString() + urlPath);

                            try {
                                try (InputStream stream = artifactUrl.openStream()) {
                                    stream.close();
                                    url = artifactUrl;
                                    break;
                                }
                            } catch (Exception ignored) {
                            }
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (url == null) {
                    throw new GradleException("Failed to find URL for artifact " + artifact);
                }

                JsonObject obj = new JsonObject();
                obj.addProperty("url", url.toString());
                try {
                    obj.addProperty("sha256", HashUtils.sha256(artifact.getFile()));
                } catch (IOException e) {
                    throw new GradleException("Failed to hash artifact", e);
                }

                rv.add(obj);
            } else {
                throw new GradleException("Library " + artifact + " cannot be used with a library configuration. Please use a maven artifact or use the embed configuration.");
            }
        });

        return rv;
    }

    private <T> JsonArray listToJsonArray(List<T> list, Function<T, JsonElement> converter) {
        JsonArray arr = new JsonArray();

        for (T entry : list) {
            arr.add(converter.apply(entry));
        }

        return arr;
    }

    public Configuration getLibraryConfiguration() {
        return libraryConfiguration;
    }

    public void setLibraryConfiguration(Configuration libraryConfiguration) {
        this.libraryConfiguration = libraryConfiguration;
    }

    public NamedDomainObjectContainer<ModExtension> getModContainer() {
        return modContainer;
    }

    public void setModContainer(NamedDomainObjectContainer<ModExtension> extensionContainer) {
        this.modContainer = extensionContainer;
    }

    public File getOutput() {
        return output;
    }

    public void setOutput(File output) {
        this.output = output;
    }
}
