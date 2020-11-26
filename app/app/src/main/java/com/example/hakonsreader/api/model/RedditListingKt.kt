package com.example.hakonsreader.api.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.jsonadapters.FloatToLongDeserializer
import com.example.hakonsreader.api.jsonadapters.ListingAdapterKt
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

@Entity
@JsonAdapter(ListingAdapterKt::class)
abstract class RedditListingKt {

    /**
     * The Unix timestamp the listing was created (ie. inserted into the database)
     */
    var insertedAt: Long = System.currentTimeMillis() / 1000L

    /**
     * The ID of the listing
     *
     * @see fullname
     */
    @PrimaryKey
    @SerializedName("id")
    lateinit var id: String

    /**
     * The "kind" of the listing
     */
    @SerializedName("kind")
    lateinit var kind: String

    /**
     * The fullname of the listing.
     *
     * A fullname is a combination of a listing's ID and its [kind]. Eg. a post
     * with ID "3gerg" will have a fullname of "t3_3gerg"
     *
     * For users this will be their username
     *
     * @see Thing
     */
    @SerializedName("name")
    lateinit var name: String

    /**
     * The URL to the listing
     */
    @SerializedName("url")
    lateinit var url: String

    /**
     * The UTC timestamp the listing was created
     */
    @SerializedName("created_utc")
    @JsonAdapter(FloatToLongDeserializer::class)
    var createdAt: Long = 0
}