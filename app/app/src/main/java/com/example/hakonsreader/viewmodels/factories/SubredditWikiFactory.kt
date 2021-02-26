package com.example.hakonsreader.viewmodels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.SubredditWikiViewModel

class SubredditWikiFactory(
    private val api: SubredditRequest
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SubredditWikiViewModel(api) as T
    }
}