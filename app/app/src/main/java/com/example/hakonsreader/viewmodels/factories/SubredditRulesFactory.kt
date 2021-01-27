package com.example.hakonsreader.viewmodels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hakonsreader.api.persistence.RedditSubredditRulesDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.SubredditRulesViewModel

class SubredditRulesFactory(
        private val subredditName: String,
        private val api: SubredditRequest,
        private val rulesDao: RedditSubredditRulesDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SubredditRulesViewModel(subredditName, api, rulesDao) as T
    }
}