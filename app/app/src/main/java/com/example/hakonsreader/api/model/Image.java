package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Class representing an image in a Reddit post. This includes both gallery items, and preview images
 * (different quality images for image posts, or thumbnails)
 */
public class Image {
    // Gallery images use u, x, y. Other images like preview images use full names

    @SerializedName(value = "url", alternate = "u")
    private String url;

    @SerializedName(value = "width", alternate = "x")
    private int width;

    @SerializedName(value = "height", alternate = "y")
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
