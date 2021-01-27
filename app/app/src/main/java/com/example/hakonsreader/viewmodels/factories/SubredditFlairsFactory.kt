package com.example.hakonsreader.viewmodels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hakonsreader.api.enums.FlairType
import com.example.hakonsreader.api.persistence.RedditFlairsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.SubredditFlairsViewModel

class SubredditFlairsFactory(
        private val subredditName: String,
        private val flairType: FlairType,
        private val api: SubredditRequest,
        private val dao: RedditFlairsDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SubredditFlairsViewModel(subredditName, flairType, api, dao) as T
    }
}