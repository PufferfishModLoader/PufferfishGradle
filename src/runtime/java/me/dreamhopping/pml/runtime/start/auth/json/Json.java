package me.dreamhopping.pml.runtime.start.auth.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Json {
    public static Object parse(String json, IntHolder pos) {
        skipWhitespace(json, pos);
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
            default:
                return parseNumber(json, pos);
        }
    }

    private static Object[] parseArray(String json, IntHolder pos) {
        skipWhitespace(json, pos);
        List<Object> list = new ArrayList<>();

        while (json.charAt(pos.value++) != ']') {
            skipWhitespace(json, pos);
            list.add(parse(json, pos));
            skipWhitespace(json, pos);
        }

        return list.toArray();
    }

    private static double parseNumber(String json, IntHolder pos) {
        skipWhitespace(json, pos);
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

    public static Map<String, Object> parseObject(String json, IntHolder pos) {
        skipWhitespace(json, pos);
        Map<String, Object> rv = new HashMap<>();

        while (json.charAt(pos.value++) != '}') {
            skipWhitespace(json, pos);
            String name = parseString(json, pos);
            skipWhitespace(json, pos);
            if (json.charAt(pos.value) == ':') pos.value++;
            skipWhitespace(json, pos);
            rv.put(name, parse(json, pos));
            skipWhitespace(json, pos);
        }

        return rv;
    }

    private static void skipWhitespace(String json, IntHolder pos) {
        while (isWhitespace(json.charAt(pos.value))) pos.value++;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private static String parseString(String json, IntHolder pos) {
        skipWhitespace(json, pos);
        pos.value++;
        StringBuilder b = new StringBuilder();
        while (json.charAt(pos.value++) != '"') {
            b.append(json.charAt(pos.value - 1));
        }
        return b.toString();
    }

    @SuppressWarnings({"unchecked"})
    public static String encode(Object value) {
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

    public static class IntHolder {
        public int value;

        public IntHolder(int value) {
            this.value = value;
        }
    }
}
