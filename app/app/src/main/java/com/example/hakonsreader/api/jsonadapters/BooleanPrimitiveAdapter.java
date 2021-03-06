package com.example.hakonsreader.api.jsonadapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Adapter to allow primitive boolean types to be converted to {@link Boolean} for null values
 */
public class BooleanPrimitiveAdapter extends TypeAdapter<Boolean> {
    @Override
    public void write(JsonWriter out, Boolean value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value);
        }
    }

    @Override
    public Boolean read(JsonReader in) throws IOException {
        final JsonToken token = in.peek();

        if (token == JsonToken.NULL) {
            return null;
        }

        // Return the actual boolean value if not null
        return in.nextBoolean();
    }
}
