package com.example.hakonsreader.api.model;

import com.example.hakonsreader.misc.SharedPreferencesManager;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.google.gson.annotations.SerializedName;

/**
 * Class representing a Reddit user
 */
public class User extends RedditListing {
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
        // Although fullnames are generally "t3_herg" etc., for users they are the actual name
        return super.getFullname();
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

    /**
     * @return The URL pointing to the profile picture image
     */
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }


    // TODO move this out of the API as this is android/application specific
    /**
     * @return Retrieves the user information stored in SharedPreferences
     */
    public static User getStoredUser() {
        return SharedPreferencesManager.get(SharedPreferencesConstants.USER_INFO, User.class);
    }

    /**
     * Stores information about a user in SharedPreferences
     *
     * @param user The object to store
     */
    public static void storeUserInfo(User user) {
        SharedPreferencesManager.put(SharedPreferencesConstants.USER_INFO, user);
    }
}
