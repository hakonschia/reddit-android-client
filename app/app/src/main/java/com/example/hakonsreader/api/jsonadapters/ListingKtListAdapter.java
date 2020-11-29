package com.example.hakonsreader.api.jsonadapters;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.RedditListingKt;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ListingKtListAdapter implements JsonDeserializer<List<RedditListingKt>> {
    @Override
    public List<RedditListingKt> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray listingsJson = (JsonArray) json;
        List<RedditListingKt> listings = new ArrayList<>();

        listingsJson.forEach(lj -> {
            RedditListingKt listing = context.deserialize(lj, RedditListingKt.class);
            listings.add(listing);
        });

        return listings;
    }
}

