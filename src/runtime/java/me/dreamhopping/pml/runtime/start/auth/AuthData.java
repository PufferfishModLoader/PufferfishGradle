package me.dreamhopping.pml.runtime.start.auth;

import me.dreamhopping.pml.runtime.start.auth.http.PostRequests;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthData {
    private final String username;
    private final String uuid;
    private final String accessToken;

    public AuthData(String username, String uuid, String accessToken) {
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
    }

    public static AuthData authenticate(String username, String password) throws IOException {
        File dataFile = new File("pg_auth_data.dat");
        if (dataFile.exists()) {
            SavedAuthData data = null;

            try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(dataFile))) {
                data = (SavedAuthData) stream.readObject();
            } catch (Throwable e) {
                System.err.println("Failed to read cached authentication data: " + e);
                dataFile.delete();
            }

            if (data != null) {
                try {
                    PostRequests.post(
                            "https://authserver.mojang.com/validate",
                            "accessToken", data.getAccessToken(),
                            "clientToken", data.getClientToken()
                    );
                    return new AuthData(data.getUsername(), data.getUuid(), data.getAccessToken());
                } catch (IllegalStateException e) {
                    try {
                        Map<String, Object> response = PostRequests.post(
                                "https://authserver.mojang.com/refresh",
                                "accessToken", data.getAccessToken(),
                                "clientToken", data.getClientToken()
                        );

                        String accessToken = (String) response.get("accessToken");
                        return saveAuthData(new AuthData(data.getUsername(), data.getUuid(), accessToken), data.getClientToken(), dataFile);
                    } catch (IllegalStateException ex) {
                        return auth(username, password, data.getClientToken(), dataFile);
                    }
                }
            }
        }
        String clientToken = UUID.randomUUID().toString();
        return auth(username, password, clientToken, dataFile);
    }

    @SuppressWarnings("unchecked")
    private static AuthData auth(String username, String password, String clientToken, File dataFile) throws IOException {
        Map<String, Object> agentMap = new HashMap<>();
        agentMap.put("name", "Minecraft");
        agentMap.put("version", 1);

        Map<String, Object> data = PostRequests.post(
                "https://authserver.mojang.com/authenticate",
                "username", username,
                "password", password,
                "clientToken", clientToken,
                "agent", agentMap
        );

        String accessToken = (String) data.get("accessToken");
        Map<String, Object> selectedProfile = (Map<String, Object>) data.get("selectedProfile");
        String uuid = (String) selectedProfile.get("id");
        String name = (String) selectedProfile.get("name");
        String uuidMsb = uuid.substring(0, 16);
        String uuidLsb = uuid.substring(16, 32);
        UUID id = new UUID(Long.parseUnsignedLong(uuidMsb, 16), Long.parseUnsignedLong(uuidLsb, 16));

        return saveAuthData(new AuthData(name, id.toString(), accessToken), clientToken, dataFile);
    }

    private static AuthData saveAuthData(AuthData data, String clientToken, File dataFile) throws IOException {
        SavedAuthData saved = new SavedAuthData(data.getAccessToken(), clientToken, data.getUsername(), data.getUuid());
        File parent = dataFile.getParentFile();
        if (parent != null) parent.mkdirs();
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            output.writeObject(saved);
        }
        return data;
    }

    public String getUsername() {
        return username;
    }

    public String getUuid() {
        return uuid;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
