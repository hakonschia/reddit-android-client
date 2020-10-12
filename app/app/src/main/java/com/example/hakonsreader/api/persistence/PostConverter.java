package com.example.hakonsreader.api.persistence;

import android.util.Log;

import androidx.room.TypeConverter;

import com.example.hakonsreader.api.model.GalleryItem;
import com.example.hakonsreader.api.model.PreviewImage;
import com.example.hakonsreader.api.model.RedditPost;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Class containing converter for various fields for Reddit posts
 */
public abstract class PostConverter {
    private static final String TAG = "CrosspostConverter";

    // TODO there has to be a better way of doing this with generics or something else


    @TypeConverter
    public static List<String> fromListString(String value) {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }
    @TypeConverter
    public static String fromList(List<String> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }


    @TypeConverter
    public static RedditPost.Media mediaFromString(String value) {
        RedditPost.Media m = new Gson().fromJson(value, RedditPost.Media.class);
        return m;
    }
    @TypeConverter
    public static String fromMedia(RedditPost.Media media) {
        Gson gson = new Gson();

        String json = gson.toJson(media);
        return gson.toJson(media);
    }

    @TypeConverter
    public static RedditPost.GalleryData galleryDataFromString(String value) {
        return new Gson().fromJson(value, RedditPost.GalleryData.class);
    }
    @TypeConverter
    public static String fromGalleryData(RedditPost.GalleryData media) {
        Gson gson = new Gson();
        return gson.toJson(media);
    }


    @TypeConverter
    public static RedditPost.Preview previewFromString(String value) {
        return new Gson().fromJson(value, RedditPost.Preview.class);
    }
    @TypeConverter
    public static String fromPreview(RedditPost.Preview preview) {
        Gson gson = new Gson();
        return gson.toJson(preview);
    }
}
