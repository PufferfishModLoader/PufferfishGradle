package dev.cbyrne.pufferfishmodloader.gradle.tasks.workspace;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.utils.HttpUtils;
import dev.cbyrne.pufferfishmodloader.gradle.utils.assets.AssetIndex;
import dev.cbyrne.pufferfishmodloader.gradle.utils.assets.AssetObject;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.VersionJson;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class TaskDownloadAssets extends DefaultTask {
    private VersionJson version;
    private PufferfishGradle plugin;

    @TaskAction
    public void download() throws IOException {
        File indexFile = new File(plugin.getCacheDir(), "assets/indexes/" + version.getAssetIndex().getId() + ".json");
        HttpUtils.download(version.getAssetIndex().getUrl(), indexFile, version.getAssetIndex().getSha1(), 5);

        AssetIndex index;
        try (FileReader reader = new FileReader(indexFile)) {
            index = PufferfishGradle.GSON.fromJson(reader, AssetIndex.class);
        }

        for (AssetObject object : index.getObjects().values()) {
            String path = object.getHash().substring(0, 2) + '/' + object.getHash();
            File file = new File(plugin.getCacheDir(), "assets/objects/" + path);
            URL url = new URL("http://resources.download.minecraft.net/" + path);
            HttpUtils.download(url, file, object.getHash(), 5);
        }
    }

    public PufferfishGradle getPlugin() {
        return plugin;
    }

    public void setPlugin(PufferfishGradle plugin) {
        this.plugin = plugin;
    }

    public VersionJson getVersion() {
        return version;
    }

    public void setVersion(VersionJson version) {
        this.version = version;
    }
}
