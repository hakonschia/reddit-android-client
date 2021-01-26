package com.example.hakonsreader.api.model.flairs

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hakonsreader.api.enums.FlairType
import com.google.gson.annotations.SerializedName

/**
 * Class representing a flair type for post submissions or user flairs on a subreddit
 */
@Entity(tableName = "flairs")
class RedditFlair {

    /**
     * The ID of the flair
     */
    @SerializedName("id")
    @PrimaryKey
    var id = ""

    /**
     * What type of flair this is (richtext or text)
     */
    @SerializedName("type")
    var type = ""

    /**
     * The raw text of the flair
     */
    @SerializedName("text")
    var text = ""

    /**
     * When [type] is "richtext", this will hold the list of richtext flairs for the flair
     */
    @SerializedName("richtext")
    var richtextFlairs = ArrayList<RichtextFlair>()

    /**
     * The text color for the flair
     */
    @SerializedName("text_color")
    var textColor = ""

    /**
     * The hex color for the background of the flair
     */
    @SerializedName("background_color")
    var backgroundColor = ""


    /**
     * The type of flair this is
     */
    var flairType: FlairType = FlairType.USER

    /**
     * The subreddit the flair is in
     */
    var subreddit = ""
}