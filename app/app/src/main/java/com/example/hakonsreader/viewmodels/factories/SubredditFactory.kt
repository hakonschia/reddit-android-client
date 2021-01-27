package com.example.hakonsreader.viewmodels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.api.persistence.RedditSubredditsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.SubredditViewModel

class SubredditFactory(
        private val subredditName: String,
        private val api: SubredditRequest,
        private val dao: RedditSubredditsDao,
        private val postsDao: RedditPostsDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SubredditViewModel(subredditName, api, dao, postsDao) as T
    }
}