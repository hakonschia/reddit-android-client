package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Class representing a preview image in a Reddit post
 */
public class PreviewImage {
    @SerializedName("url")
    private String url;

    @SerializedName("width")
    private int width;

    @SerializedName("height")
    private int height;


    /**
     * @return The URL to the image
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return The width of the image
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The height of the image
     */
    public int getHeight() {
        return height;
    }
}
