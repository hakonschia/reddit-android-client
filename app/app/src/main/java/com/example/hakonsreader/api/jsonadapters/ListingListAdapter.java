package com.example.hakonsreader.api.jsonadapters;

import com.example.hakonsreader.api.model.RedditListing;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ListingListAdapter implements JsonDeserializer<List<RedditListing>> {
    @Override
    public List<RedditListing> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray listingsJson = (JsonArray) json;
        List<RedditListing> listings = new ArrayList<>();

        listingsJson.forEach(lj -> {
            RedditListing listing = context.deserialize(lj, RedditListing.class);
            listings.add(listing);
        });

        return listings;
    }
}
