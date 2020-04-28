package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import com.google.gson.annotations.SerializedName;

public class DownloadsContainer {
    private final Artifact client;
    @SerializedName("client_mappings") private final Artifact clientMappings;
    private final Artifact server;
    @SerializedName("server_mappings") private final Artifact serverMappings;

    public DownloadsContainer(Artifact client, Artifact clientMappings, Artifact server, Artifact serverMappings) {
        this.client = client;
        this.clientMappings = clientMappings;
        this.server = server;
        this.serverMappings = serverMappings;
    }

    @Override
    public String toString() {
        return "DownloadsContainer{" +
                "client=" + client +
                ", clientMappings=" + clientMappings +
                ", server=" + server +
                ", serverMappings=" + serverMappings +
                '}';
    }

    public Artifact getClient() {
        return client;
    }

    public Artifact getClientMappings() {
        return clientMappings;
    }

    public Artifact getServer() {
        return server;
    }

    public Artifact getServerMappings() {
        return serverMappings;
    }
}
