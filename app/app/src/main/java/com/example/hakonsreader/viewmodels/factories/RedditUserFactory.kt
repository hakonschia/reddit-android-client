package com.example.hakonsreader.viewmodels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hakonsreader.viewmodels.RedditUserViewModel

class RedditUserFactory(
        val username: String?,
        val isForLoggedInUser: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RedditUserViewModel(username, isForLoggedInUser) as T
    }
}