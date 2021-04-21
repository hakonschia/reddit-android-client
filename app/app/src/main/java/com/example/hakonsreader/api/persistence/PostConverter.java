package com.example.hakonsreader.api.persistence;

import androidx.room.TypeConverter;

import com.example.hakonsreader.api.model.RedditAward;
import com.example.hakonsreader.api.model.images.RedditGalleryItem;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.flairs.RichtextFlair;
import com.example.hakonsreader.api.model.internal.GalleryData;
import com.example.hakonsreader.api.utils.UtilKt;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Class containing converter for various fields for Reddit posts
 */
public class PostConverter {
    private static final String TAG = "CrosspostConverter";

    private static final Gson gson = new Gson();

    // TODO there has to be a better way of doing this with generics or something else


    @TypeConverter
    public static List<String> toListString(String value) {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }
    @TypeConverter
    public static String fromListString(List<String> list) {
        return gson.toJson(list);
    }


    @TypeConverter
    public static List<RedditGalleryItem> toGalleryItemListString(String value) {
        Type listType = new TypeToken<ArrayList<RedditGalleryItem>>() {}.getType();
        return gson.fromJson(value, listType);
    }
    @TypeConverter
    public static String fromGalleryItemListString(List<RedditGalleryItem> list) {
        return gson.toJson(list);
    }


    @TypeConverter
    public static RedditPost.Media mediaFromString(String value) {
        return gson.fromJson(value, RedditPost.Media.class);
    }
    @TypeConverter
    public static String fromMedia(RedditPost.Media media) {
        return gson.toJson(media);
    }


    // These converters are used for values such as media_metadata that hold an object that has
    // an unknown amount of anonymous objects (these objects can be converted to a LinkedTreeMap)
    @TypeConverter
    public static LinkedTreeMap<String, Object> linkedTreeMapFromString(String value) {
        Type listType = new TypeToken<LinkedTreeMap<String, Object>>() {}.getType();
        return gson.fromJson(value, listType);
    }
    @TypeConverter
    public static String fromLinkedTreeMap(LinkedTreeMap<String, Object> map) {
        return gson.toJson(map);
    }

    @TypeConverter
    public static GalleryData galleryDataFromString(String value) {
        return gson.fromJson(value, GalleryData.class);
    }
    @TypeConverter
    public static String fromGalleryData(GalleryData data) {
        return gson.toJson(data);
    }


    @TypeConverter
    public static RedditPost.Preview previewFromString(String value) {
        return gson.fromJson(value, RedditPost.Preview.class);
    }
    @TypeConverter
    public static String fromPreview(RedditPost.Preview preview) {
        return gson.toJson(preview);
    }

    @TypeConverter
    public static ArrayList<RichtextFlair> richTextFlairListFromString(String value) {
        Type listType = new TypeToken<ArrayList<RichtextFlair>>() {}.getType();
        return gson.fromJson(value, listType);
    }
    @TypeConverter
    public static String fromRichTextFlairList(List<RichtextFlair> images) {
        return gson.toJson(images);
    }


    @TypeConverter
    public static List<RedditAward> redditAwardListFromString(String value) {
        Type listType = new TypeToken<List<RedditAward>>() {}.getType();
        return gson.fromJson(value, listType);
    }
    @TypeConverter
    public static String fromRedditAwardList(List<RedditAward> images) {
        return gson.toJson(images);
    }



    @TypeConverter
    public static Object[][] arrayFromString(String value) {
        Type listType = new TypeToken<Object[][]>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String stringFromArray(Object[][] array) {
        return gson.toJson(array);
    }


    @TypeConverter
    public static Object objectFromString(String value) {
        Object thirdParty = UtilKt.thirdPartyObjectFromJsonString(value);
        if (thirdParty != null) {
            return thirdParty;
        }

        Type listType = new TypeToken<Object>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String stringFromObject(Object object) {
        try {
            // Since this is a generic converter for any type of object, add the class name so it can be
            // restored correctly later
            JsonObject json = gson.toJsonTree(object).getAsJsonObject();
            json.addProperty("type", object.getClass().getSimpleName());
            return json.toString();
        } catch (IllegalStateException | JsonParseException e) {
            return null;
        }
    }
}
