package me.dreamhopping.pml.runtime.start.auth;

import java.io.Serializable;

public class SavedAuthData implements Serializable {
    private final String accessToken;
    private final String clientToken;
    private final String username;
    private final String uuid;

    public SavedAuthData(String accessToken, String clientToken, String username, String uuid) {
        this.accessToken = accessToken;
        this.clientToken = clientToken;
        this.username = username;
        this.uuid = uuid;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getClientToken() {
        return clientToken;
    }

    public String getUsername() {
        return username;
    }

    public String getUuid() {
        return uuid;
    }
}
