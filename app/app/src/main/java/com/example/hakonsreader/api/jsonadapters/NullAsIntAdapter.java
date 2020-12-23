package com.example.hakonsreader.api.jsonadapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Deserializer that converts {@code null} to {@code 0}
 */
public class NullAsIntAdapter extends TypeAdapter<Integer> {

    @Override
    public void write(JsonWriter out, Integer value) throws IOException {
        out.value(value);
    }

    @Override
    public Integer read(JsonReader in) throws IOException {
        final JsonToken token = in.peek();

        // Return 0 instead of NULL
        if (token == JsonToken.NULL) {
            return 0;
        }

        // Return the actual value if not null
        return in.nextInt();
    }
}
