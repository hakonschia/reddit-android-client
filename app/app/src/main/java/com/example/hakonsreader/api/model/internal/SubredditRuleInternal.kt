package com.example.hakonsreader.api.model.internal

import com.example.hakonsreader.api.model.SubredditRule
import com.google.gson.annotations.SerializedName

class SubredditRuleInternal {

    @SerializedName("rules")
    val rules: List<SubredditRule>? = null
}