package com.example.hakonsreader.api.jsonadapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;

/**
 * Adapter to convert empty strings into null objects
 *
 * @param <T> The type to convert to
 */
public class EmptyStringAsNullAdapter<T> implements JsonDeserializer<T> {
    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            JsonPrimitive primitive = json.getAsJsonPrimitive();

            if (primitive.isString() && primitive.getAsString().isEmpty()) {
                return null;
            }
        }

        return context.deserialize(json, typeOfT);
    }
}