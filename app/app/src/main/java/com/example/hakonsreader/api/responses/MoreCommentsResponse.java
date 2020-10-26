package com.example.hakonsreader.api.responses;

import android.util.Log;

import com.example.hakonsreader.api.model.RedditComment;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Response for retrieving more comments in a comment chain, and for adding comments to a chain
 * (to a post or a comment chain)
 *
 * <p>The json returned for these requests differs from the standard listing response and as such
 * needs its own class</p>
 */
public class MoreCommentsResponse {

    // Instead of an anonymous object the object is called "json"
    // There is also an "errors" list, but I


    @SerializedName("json")
    private ListingResponse comments;

    /**
     * @return The list of more comments
     */
    public List<RedditComment> getComments() {
        return (List<RedditComment>) comments.getListings();
    }

    public boolean hasErrors() {
        return comments.hasErrors();
    }

    public List<List<String>> errors() {
        return comments.getErrors();
    }

    private class Des implements JsonDeserializer<List<RedditComment>> {

        @Override
        public List<RedditComment> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray data = json.getAsJsonArray();
            Log.d("JSONDES", "deserialize: "+ data);
            return context.deserialize(data, typeOfT);
        }
    }
}
