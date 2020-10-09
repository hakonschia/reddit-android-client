package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

public class RedditVideo {
    @SerializedName("duration")
    private int duration;

    @SerializedName("fallback_url")
    private String fallbackURL;

    @SerializedName("dash_url")
    private String dashURL;

    @SerializedName("hls_url")
    private String hlsURL;

    @SerializedName("height")
    private int height;

    @SerializedName("width")
    private int width;

    @SerializedName("is_gif")
    private boolean isGif;


    public int getDuration() {
        return duration;
    }

    /**
     * Gets the fallback URL for the video
     */
    public String getFallbackURL() {
        return fallbackURL;
    }

    /**
     * Gets the url to the DASH (Dynamic Adaptive Streaming over HTTP) video for the post
     */
    public String getDashUrl() {
        return dashURL;
    }

    /**
     * Gets the URL to the HLS (HTTP Live Streaming) video for the post
     */
    public String getHlsURL() {
        return hlsURL;
    }


    /**
     * Gets the width of the video
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height of the video
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return True if the video is a gif
     */
    public boolean isGif() {
        return isGif;
    }
}
