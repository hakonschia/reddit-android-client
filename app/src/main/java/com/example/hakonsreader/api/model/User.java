package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Class representing a reddit user
 */
public class User {
    private String name;

    @SerializedName("comment_karma")
    private int commentKarma;

    @SerializedName("link_karma")
    private int postKarma;

    @SerializedName("pref_video_autoplay")
    private boolean autoPlayVideos;

    @SerializedName("icon_img")
    private String profilePictureUrl;


    public String getName() {
        return name;
    }

    public int getCommentKarma() {
        return commentKarma;
    }

    public int getPostKarma() {
        return postKarma;
    }
}
