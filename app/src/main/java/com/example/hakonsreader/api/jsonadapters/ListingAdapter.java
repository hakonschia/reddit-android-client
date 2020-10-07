package com.example.hakonsreader.api.jsonadapters;

import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.Subreddit;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;


/**
 * Json deserializer that automatically converts a listing to its appropriate class
 */
public class ListingAdapter implements JsonDeserializer<RedditListing> {
    @Override
    public RedditListing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String kind = json.getAsJsonObject().get("kind").getAsString();
        JsonObject data = json.getAsJsonObject().getAsJsonObject("data");

        // Set the kind in the innner object instead so it isn't lost
        data.addProperty("kind", kind);

        RedditListing listing;
        // So far we only support posts, comments, and subreddits
        if (Thing.POST.getValue().equals(kind)) {
            listing = context.deserialize(data, RedditPost.class);
        } else if (Thing.COMMENT.getValue().equals(kind) || Thing.MORE.getValue().equals(kind)) {
            // So far at least "more" kinds are only comments
            listing = context.deserialize(data, RedditComment.class);
        } else {
            listing = context.deserialize(data, Subreddit.class);
        }

        return listing;
    }
}
