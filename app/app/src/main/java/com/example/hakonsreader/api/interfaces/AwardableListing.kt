package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.RedditAward

interface AwardableListing {

    /**
     * The ID of the listing
     */
    var id: String

    /**
     * The list of awardings this listing has
     */
    var awardings: List<RedditAward>?
}