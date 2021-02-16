package com.example.hakonsreader.viewmodels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel

class SelectSubredditsFactory(
        private val isForLoggedInUser: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SelectSubredditsViewModel(isForLoggedInUser) as T
    }
}