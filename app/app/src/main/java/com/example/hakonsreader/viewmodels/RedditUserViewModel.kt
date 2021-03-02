package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.responses.ApiResponse
import kotlinx.coroutines.launch

class RedditUserViewModel(
        val username: String?,
        private val isForLoggedInUser: Boolean
) : ViewModel() {

    private val api = App.get().api

    private var infoLoaded = false

    private val _user = MutableLiveData<RedditUser>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    val user: LiveData<RedditUser> = _user
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error

    init {
        // This is probably a pretty bad way to get the current user info, but whatever
        if (isForLoggedInUser) {
            App.get().currentUserInfo?.userInfo?.let {
                _user.postValue(it)
            }
        }
    }


    fun load() {
        if (infoLoaded) {
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            // If we're privately browsing, attempting to get user info for a logged in user would
            // fail with a "You're currently privately browsing"
            // If the user is privately browsing but no name is previously set this would fail since name would be null
            // But that should never happen? A logged in user should always have a user object with name stored
            val response = if ((isForLoggedInUser && !App.get().isUserLoggedInPrivatelyBrowsing()) || username == null) {
                api.user().info()
            } else {
                api.user(username).info()
            }

            _isLoading.postValue(false)

            when (response) {
                is ApiResponse.Success -> {
                    infoLoaded = true
                    _user.postValue(response.value!!)
                }
                is ApiResponse.Error -> _error.postValue(ErrorWrapper(response.error, response.throwable))
            }
        }
    }

}