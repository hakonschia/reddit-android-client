package com.example.hakonsreader.viewmodels

import androidx.lifecycle.*
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.states.AppState
import com.example.hakonsreader.states.LoggedInState
import kotlinx.coroutines.launch

/**
 * ViewModel for retrieving information about a Reddit user
 */
class RedditUserViewModel(
        private val username: String?,
        private val isForLoggedInUser: Boolean,
        private val api: RedditApi,
) : ViewModel() {

    /**
     * Factory class for the ViewModel
     *
     * @param username The username of the user to load information for. This can be `null` if the
     * ViewModel is for the logged in user
     * @param isForLoggedInUser
     * @param api The API to use
     */
    class Factory(
            private val username: String?,
            private val isForLoggedInUser: Boolean,
            private val api: RedditApi,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RedditUserViewModel(username, isForLoggedInUser, api) as T
        }
    }

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
            AppState.getUserInfo()?.userInfo?.let {
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
            val response = if ((isForLoggedInUser && AppState.loggedInState.value !is LoggedInState.PrivatelyBrowsing) || username == null) {
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