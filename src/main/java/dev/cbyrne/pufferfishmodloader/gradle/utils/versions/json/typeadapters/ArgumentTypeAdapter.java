package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.typeadapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.Argument;
import dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json.Rule;

import java.io.IOException;

public class ArgumentTypeAdapter extends TypeAdapter<Argument> {
    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() == Argument.class) {
                return (TypeAdapter<T>) new ArgumentTypeAdapter(gson);
            }
            return null;
        }
    };
    private final Gson gson;

    public ArgumentTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, Argument value) throws IOException {
        if (value.getValue() == null || value.getValue().length < 1) throw new IllegalArgumentException("Invalid argument");
        if ((value.getRules() == null || value.getRules().length == 0) && value.getValue().length == 1) {
            out.value(value.getValue()[0]);
        } else {
            out.beginObject();
            if (value.getRules() != null && value.getRules().length > 0) {
                out.name("rules");
                out.beginArray();

                TypeAdapter<Rule> ruleAdapter = gson.getAdapter(Rule.class);
                for (Rule rule : value.getRules()) {
                    ruleAdapter.write(out, rule);
                }

                out.endArray();
            }

            out.name("value");
            if (value.getValue().length == 1) {
                out.value(value.getValue()[0]);
            } else {
                out.beginArray();
                for (String v : value.getValue()) {
                    out.value(v);
                }
                out.endArray();
            }

            out.endObject();
        }
    }

    @Override
    public Argument read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.STRING) {
            return new Argument(new String[] { in.nextString() }, new Rule[0]);
        } else {
            in.beginObject();

            Rule[] rules = new Rule[0];
            String[] value = new String[0];

            TypeAdapter<Rule[]> ruleArrayTypeAdapter = gson.getAdapter(Rule[].class);

            while (in.hasNext()) {
                String name = in.nextName();
                if (name.equalsIgnoreCase("rules")) {
                    rules = ruleArrayTypeAdapter.read(in);
                } else if (name.equalsIgnoreCase("value")) {
                    value = parseValue(in);
                }
            }

            in.endObject();
            return new Argument(value, rules);
        }
    }

    private String[] parseValue(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.STRING) {
            return new String[] { in.nextString() };
        } else {
            TypeAdapter<String[]> stringArrayTypeAdapter = gson.getAdapter(String[].class);
            return stringArrayTypeAdapter.read(in);
        }
    }
}
