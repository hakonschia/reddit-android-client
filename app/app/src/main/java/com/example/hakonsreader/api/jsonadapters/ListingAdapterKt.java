package com.example.hakonsreader.api.jsonadapters;

import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.RedditListingKt;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditUser;
import com.example.hakonsreader.api.model.Subreddit;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

public class ListingAdapterKt implements JsonDeserializer<RedditListingKt> {
    private static final String TAG = "ListingAdapter";

    @Override
    public RedditListingKt deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        String kind = json.getAsJsonObject().get("kind").getAsString();
        JsonObject data = json.getAsJsonObject().getAsJsonObject("data");

        // For some reason, when going into ReplyActivity this (only sometimes) causes a NPE because
        // data is null
        if (data == null) {
            data = new JsonObject();
        }

        // Set the kind in the inner object instead so it isn't lost
        data.addProperty("kind", kind);

        RedditListingKt listing = context.deserialize(data, RedditUser.class);
        /*
        if (Thing.ACCOUNT.getValue().equals(kind)) {
            listing = context.deserialize(data, RedditUser.class);
        }
         */

        return listing;
    }
}
