package com.example.hakonsreader.api.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.jsonadapters.FloatToLongDeserializer
import com.example.hakonsreader.api.jsonadapters.ListingAdapter
import com.google.gson.annotations.JsonAdapter

@Entity
@JsonAdapter(ListingAdapter::class)
abstract class RedditListingKt {

    /**
     * The timestamp the listing was created at (ie. inserted into the local database)
     */
    var insertedAt = System.currentTimeMillis() / 1000

    /**
     * The ID of the listing. The ID of a listing is unique across that type of listings (eg.
     * a post ID is unique across all posts)
     */
    @PrimaryKey
    @SerializedName("id")
    var id: String = ""

    /**
     * The type of listing this is (eg. *t3* for posts)
     *
     * The types of listings available are represented in [Thing]
     */
    @SerializedName("kind")
    var kind: String = ""

    /**
     * The name of the listing.
     *
     * This is typically what is referred to as a *fullname* by Reddit. A fullname if the listings
     * [kind] combined with its [id], eg. *t3_rerrg* for a post.
     *
     * For users this will represent the users username instead of the fullname
     */
    @SerializedName("name")
    var name: String = ""

    /**
     * The URL pointing to the listing
     */
    // TODO this isn't applicable for users
    /*
    @SerializedName("url")
    var url: String = ""
     */

    /**
     * The UTC timestamp the listing was created
     */
    @SerializedName("created_utc")
    @JsonAdapter(FloatToLongDeserializer::class)
    var createdAt = -1L

    /**
     * True if the listing is marked as Not Safe For Work (18+)
     */
    // TODO this isn't applicable for all types of listings (like comments)
    /*
    @SerializedName(value = "over_18", alternate = ["over18"])
    var isNsfw = false
     */
}