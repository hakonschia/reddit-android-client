package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

// TODO submit response: {"json": {"errors": [], "data": {"url": "https://www.reddit.com/r/hakonschia/comments/k1yj0s/hello_reddit/", "drafts_count": 0, "id": "k1yj0s", "name": "t3_k1yj0s"}}}
//  can probably just return the ID of the post
class Submission {

    @SerializedName("url")
    var url = ""

    @SerializedName("id")
    var id = ""

    @SerializedName("name")
    var fullname = ""

    @SerializedName("drafts_count")
    var draftsCount = 0

}