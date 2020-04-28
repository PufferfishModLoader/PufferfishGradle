package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import java.util.Arrays;

public class Argument {
    private final String[] value;
    private final Rule[] rules;

    public Argument(String[] value, Rule[] rules) {
        this.value = value;
        this.rules = rules;
    }

    public String[] getValue() {
        return value;
    }

    public Rule[] getRules() {
        return rules;
    }

    @Override
    public String toString() {
        return "Argument{" +
                "value=" + Arrays.toString(value) +
                ", rules=" + Arrays.toString(rules) +
                '}';
    }
}
