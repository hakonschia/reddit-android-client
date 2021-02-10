package com.example.hakonsreader.api.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.jsonadapters.FloatToLongDeserializer
import com.example.hakonsreader.api.jsonadapters.ListingAdapter
import com.google.gson.annotations.JsonAdapter

@Entity
@JsonAdapter(ListingAdapter::class)
abstract class RedditListing {

    /**
     * The timestamp the listing was created at locally (ie. inserted into the local database)
     */
    var insertedAt = System.currentTimeMillis() / 1000

    /**
     * The ID of the listing. The ID of a listing is unique across that type of listings (eg.
     * a post ID is unique across all posts)
     */
    @PrimaryKey
    @SerializedName("id")
    var id = ""

    /**
     * The type of listing this is (eg. *t3* for posts)
     *
     * The types of listings available are represented in [Thing]
     */
    @SerializedName("kind")
    var kind = ""

    /**
     * The fullname of the listing. A fullname is the listings [kind] combined with its [id], eg. *t3_rerrg* for a post.
     */
    open val fullname get() = "${kind}_$id"

    /**
     * The UTC timestamp the listing was created
     */
    @SerializedName("created_utc")
    @JsonAdapter(FloatToLongDeserializer::class)
    var createdAt = -1L
}