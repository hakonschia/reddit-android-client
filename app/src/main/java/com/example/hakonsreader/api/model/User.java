package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Class representing a Reddit user
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

    /**
     * @return The username of the user
     */
    public String getName() {
        return name;
    }

    /**
     * @return The amount of comment karma the user has
     */
    public int getCommentKarma() {
        return commentKarma;
    }

    /**
     * @return The amount of post karma the user has
     */
    public int getPostKarma() {
        return postKarma;
    }
}
