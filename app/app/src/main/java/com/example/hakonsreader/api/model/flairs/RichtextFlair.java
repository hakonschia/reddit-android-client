package com.example.hakonsreader.api.model.flairs;


import com.google.gson.annotations.SerializedName;

/**
 * A richtext flair is a flair combined of multiple types of items, such as images and text.
 * This class represents one of the items in a richtext flair
 */
public class RichtextFlair {
    @SerializedName("e")
    private String type;

    @SerializedName("t")
    private String text;

    @SerializedName("u")
    private String url;


    /**
     * @return The type of flair (such as emoji, text)
     */
    public String getType() {
        return type;
    }

    /**
     * @return The text for the flair (for {@link RichtextFlair#getType()} == text)
     */
    public String getText() {
        return text;
    }

    /**
     * @return The URL for an icon in the flair (for {@link RichtextFlair#getType()} == emoji)
     */
    public String getUrl() {
        return url;
    }
}