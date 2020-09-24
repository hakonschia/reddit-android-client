package com.example.hakonsreader.api.model.flairs;


import com.google.gson.annotations.SerializedName;

/**
 * Class representing a richtext flair
 */
public class RichtextFlair {
    @SerializedName("e")
    private String e;

    @SerializedName("t")
    private String text;


    public String getText() {
        return text;
    }
}