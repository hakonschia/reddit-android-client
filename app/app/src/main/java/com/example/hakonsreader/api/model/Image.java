package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

/**
 * Class representing an image in a Reddit post. This includes both gallery items, and preview images
 * (different quality images for image posts, or thumbnails). This might also be linking to a gif URL
 */
public class Image {
    // Gallery images use u, x, y. Other images like preview images use full names
    // Imgur images use width, height and "link" instead of "url"

    @SerializedName(value = "url", alternate = {"u", "link"})
    private String url;

    @SerializedName(value = "width", alternate = "x")
    private int width;

    @SerializedName(value = "height", alternate = "y")
    private int height;

    @SerializedName("m")
    private String mimeType;

    @SerializedName("gif")
    private String gifUrl;

    @SerializedName("mp4")
    private String mp4Url;


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

    /**
     * @return Gets the image mimetype
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * If this is image is in a Reddit gallery and the image is a gif, this will contain the link to
     * the mp4 URL
     */
    @Nullable
    public String getGifUrl() {
        return gifUrl;
    }

    /**
     * If this is image is in a Reddit gallery and the image is a gif, this will contain the link to
     * the mp4 URL
     */
    @Nullable
    public String getMp4Url() {
        return mp4Url;
    }

    public boolean isGif() {
        return gifUrl != null;
    }
}
