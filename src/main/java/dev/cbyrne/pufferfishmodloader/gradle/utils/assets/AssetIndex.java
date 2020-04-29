package dev.cbyrne.pufferfishmodloader.gradle.utils.assets;

import java.util.Map;

public class AssetIndex {
    private final Map<String, AssetObject> objects;

    public AssetIndex(Map<String, AssetObject> objects) {
        this.objects = objects;
    }

    public Map<String, AssetObject> getObjects() {
        return objects;
    }
}
