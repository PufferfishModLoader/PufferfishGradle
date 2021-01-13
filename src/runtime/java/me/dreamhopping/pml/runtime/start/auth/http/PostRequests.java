package me.dreamhopping.pml.runtime.start.auth.http;

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
        String jsonData = encode(buildMap(parameters));

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
            data = readFully(stream);
        }

        if (successful && data.length() == 0) return new HashMap<>();

        Map<String, Object> d = parseObject(data, new IntHolder(0));
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

    private static Object parse(String json, IntHolder pos) {
        switch (json.charAt(pos.value)) {
            case '{': return parseObject(json, pos);
            case '[': return parseArray(json, pos);
            case '"': return parseString(json, pos);
            case 't':
                pos.value += 4;
                return true;
            case 'f':
                pos.value += 5;
                return false;
            default: return parseNumber(json, pos);
        }
    }

    private static Object[] parseArray(String json, IntHolder pos) {
        List<Object> list = new ArrayList<>();

        while (json.charAt(pos.value++) != ']') {
            list.add(parse(json, pos));
        }

        return list.toArray();
    }

    private static double parseNumber(String json, IntHolder pos) {
        boolean negative = json.charAt(pos.value) == '-';
        if (negative) pos.value++;
        String s = parseDecimals(json, pos);
        if (json.charAt(pos.value) == '.') {
            pos.value++;
            s += "." + parseDecimals(json, pos);
        }
        double value = Double.parseDouble(s);
        if (negative) return -value;
        return value;
    }
    
    private static String parseDecimals(String json, IntHolder pos) {
        StringBuilder rv = new StringBuilder();
        while (isNumeric(json.charAt(pos.value++))) {
            rv.append(json.charAt(pos.value - 1));
        }
        return rv.toString();
    }
    
    private static boolean isNumeric(char c) {
        return c >= '0' && c <= '9';
    }
    
    private static Map<String, Object> parseObject(String json, IntHolder pos) {
        Map<String, Object> rv = new HashMap<>();
        
        while (json.charAt(pos.value++) != '}') {
            String name = parseString(json, pos);
            if (json.charAt(pos.value) == ':') pos.value++;
            rv.put(name, parse(json, pos));
        }
        
        return rv;
    }
    
    private static String parseString(String json, IntHolder pos) {
        pos.value++;
        StringBuilder b = new StringBuilder();
        while (json.charAt(pos.value++) != '"') {
            b.append(json.charAt(pos.value - 1));
        }
        return b.toString();
    }
    
    private static String readFully(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[4096];
        while (true) {
            int i = stream.read(buffer, 0, buffer.length);
            if (i < 0) break;
            builder.append(new String(buffer, 0, buffer.length, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    @SuppressWarnings({"unchecked"})
    private static String encode(Object value) {
        if (value instanceof String) {
            return '"' + ((String) value) + '"';
        } else if (value == null) {
            return "null";
        }  else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                if (!first) builder.append(',');
                String name = entry.getKey().toString();
                builder.append(encode(name));
                builder.append(':');
                builder.append(encode(entry.getValue()));
                first = false;
            }
            builder.append('}');
            return builder.toString();
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? "true" : "false";
        } else if (value.getClass().isArray()) {
            StringBuilder builder = new StringBuilder("[");

            boolean first = true;
            for (Object o : (Object[]) value) {
                if (!first) builder.append(',');
                builder.append(encode(o));
                first = false;
            }

            return builder.toString();
        } else {
            throw new IllegalArgumentException("cannot serialize " + value.getClass().getName());
        }
    }
    
    private static class IntHolder {
        public int value;

        public IntHolder(int value) {
            this.value = value;
        }
    }
}
