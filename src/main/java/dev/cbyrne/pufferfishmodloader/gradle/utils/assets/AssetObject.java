package dev.cbyrne.pufferfishmodloader.gradle.utils.assets;

import java.io.Serializable;

public class AssetObject implements Serializable {
    private final String hash;
    private final int size;

    public AssetObject(String hash, int size) {
        this.hash = hash;
        this.size = size;
    }

    public String getHash() {
        return hash;
    }

    public int getSize() {
        return size;
    }
}
