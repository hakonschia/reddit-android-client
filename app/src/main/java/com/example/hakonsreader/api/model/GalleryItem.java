package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Class representing the items in a gallery post
 */
public class GalleryItem {
    /**
     * The media ID of the item
     */
    @SerializedName("media_id")
    private String mediaID;

    /**
     * The ID of the item
     */
    @SerializedName("id")
    private int id;
}
