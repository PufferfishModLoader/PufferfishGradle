package me.dreamhopping.pml.runtime.start.auth.http;

import me.dreamhopping.pml.runtime.start.auth.io.IOUtil;
import me.dreamhopping.pml.runtime.start.auth.json.Json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostRequests {
    public static Map<String, Object> post(String url, Object... parameters) throws IOException {
        String jsonData = Json.encode(buildMap(parameters));

        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "PufferfishGradle/" + PostRequests.class.getPackage().getImplementationVersion());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        ((HttpURLConnection) connection).setRequestMethod("POST");
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(jsonData.getBytes(StandardCharsets.UTF_8));
        }
        String data;
        boolean successful = ((HttpURLConnection) connection).getResponseCode() / 100 == 2;
        try (InputStream stream = successful ? connection.getInputStream() : ((HttpURLConnection) connection).getErrorStream()) {
            data = IOUtil.readFully(stream);
        }

        if (successful && data.length() == 0) return new HashMap<>();

        Map<String, Object> d = Json.parseObject(data, new Json.IntHolder(0));
        if (successful) {
            return d;
        } else {
            throw new IllegalStateException((String) d.get("errorMessage"));
        }
    }

    private static Map<String, Object> buildMap(Object... params) {
        Map<String, Object> value = new HashMap<>();

        for (int i = 0; i < params.length; i += 2) {
            value.put(params[i].toString(), params[i + 1]);
        }

        return value;
    }
}
