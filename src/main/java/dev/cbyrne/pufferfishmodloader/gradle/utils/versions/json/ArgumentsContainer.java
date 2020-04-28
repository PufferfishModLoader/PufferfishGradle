package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import java.util.Arrays;

public class ArgumentsContainer {
    private final Argument[] game;
    private final Argument[] jvm;

    public ArgumentsContainer(Argument[] game, Argument[] jvm) {
        this.game = game;
        this.jvm = jvm;
    }

    @Override
    public String toString() {
        return "ArgumentsContainer{" +
                "game=" + Arrays.toString(game) +
                ", jvm=" + Arrays.toString(jvm) +
                '}';
    }

    public Argument[] getGame() {
        return game;
    }

    public Argument[] getJvm() {
        return jvm;
    }
}
