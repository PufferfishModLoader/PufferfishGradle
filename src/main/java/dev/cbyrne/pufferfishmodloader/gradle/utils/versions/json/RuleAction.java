package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import com.google.gson.annotations.SerializedName;

public enum RuleAction {
    @SerializedName("allow") ALLOW,
    @SerializedName("disallow") DENY
}
