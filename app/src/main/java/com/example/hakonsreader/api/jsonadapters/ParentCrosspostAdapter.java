package com.example.hakonsreader.api.jsonadapters;


import android.util.Log;

import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditPost;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to convert the list of parent crossposts a post has
 */
public class ParentCrosspostAdapter implements JsonDeserializer<List<RedditPost>>, JsonSerializer<List<RedditPost>> {
    private static final String TAG = "CrosspostAdapter";


    // In the JSON returned from Reddit the posts found in crossposts ONLY include the data, not "kind: t3, data: {}"
    // To simplify things greatly and use it as every other post, so under the deserialization the missing fields
    // are added
    // For serialization the fields are removed again so that we can internally serialize and deserialize without issues


    @Override
    public List<RedditPost> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        if (!json.isJsonArray()) {
            return new ArrayList<>();
        }
        JsonArray array = new JsonArray();

        JsonArray elements = json.getAsJsonArray();
        for (JsonElement element : elements) {
            JsonObject obj = new JsonObject();
            obj.addProperty("kind", Thing.POST.getValue());
            obj.add("data", element.getAsJsonObject());
            array.add(obj);
        }

        return context.deserialize(array, typeOfT);
    }

    @Override
    public JsonElement serialize(List<RedditPost> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();

        Gson gson = new Gson();

        for (RedditPost post : src) {
            array.add(gson.toJsonTree(post.data));
        }

        return array;
    }
}