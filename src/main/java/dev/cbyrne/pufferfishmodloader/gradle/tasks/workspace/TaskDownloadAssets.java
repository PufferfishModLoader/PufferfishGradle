package dev.cbyrne.pufferfishmodloader.gradle.tasks.workspace;

import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;
import dev.cbyrne.pufferfishmodloader.gradle.utils.HttpUtils;
import dev.cbyrne.pufferfishmodloader.gradle.utils.assets.AssetIndex;
import dev.cbyrne.pufferfishmodloader.gradle.utils.assets.AssetObject;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class TaskDownloadAssets extends DefaultTask {
    @Input
    private AssetIndex assetIndex;
    @Internal
    private PufferfishGradle plugin;

    @TaskAction
    public void download() throws IOException {
        for (AssetObject object : assetIndex.getObjects().values()) {
            String path = object.getHash().substring(0, 2) + '/' + object.getHash();
            File file = new File(plugin.getCacheDir(), "assets/objects/" + path);
            URL url = new URL("http://resources.download.minecraft.net/" + path);
            HttpUtils.download(url, file, object.getHash(), 5, true);
        }
    }

    public PufferfishGradle getPlugin() {
        return plugin;
    }

    public void setPlugin(PufferfishGradle plugin) {
        this.plugin = plugin;
    }

    public AssetIndex getAssetIndex() {
        return assetIndex;
    }

    public void setAssetIndex(AssetIndex assetIndex) {
        this.assetIndex = assetIndex;

        for (AssetObject object : assetIndex.getObjects().values()) {
            String path = object.getHash().substring(0, 2) + '/' + object.getHash();
            File file = new File(plugin.getCacheDir(), "assets/objects/" + path);
            getOutputs().file(file);
        }
    }
}
