package com.example.hakonsreader.viewmodels

import androidx.lifecycle.*
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.states.AppState
import com.example.hakonsreader.states.LoggedInState
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

/**
 * ViewModel for retrieving information about a Reddit user
 */
class RedditUserViewModel @AssistedInject constructor(
        @Assisted private val username: String?,
        @Assisted private val isForLoggedInUser: Boolean,
        @Assisted private val savedStateHandle: SavedStateHandle,
        private val api: RedditApi,
) : ViewModel() {

    companion object {
        private const val SAVED_USER_INFO = "saved_userInfo"
    }

    @AssistedFactory
    interface Factory {
        /**
         * Factory for the ViewModel
         *
         * @param username The username of the user to load information for. This can be `null` if the
         * ViewModel is for the logged in user
         * @param isForLoggedInUser Set to true if the ViewModel is for the logged in user
         */
        fun create(
                username: String?,
                isForLoggedInUser: Boolean,
                savedStateHandle: SavedStateHandle
        ): RedditUserViewModel
    }


    private var infoLoaded = false

    private val _user = MutableLiveData<RedditUser>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    val user: LiveData<RedditUser> = _user
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error

    init {
        val userInfo: String? = savedStateHandle.get(SAVED_USER_INFO)
        if (userInfo != null) {
            val user = Gson().fromJson(userInfo, RedditUser::class.java)
            _user.postValue(user)
        } else if (isForLoggedInUser) {
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
                    savedStateHandle.set(SAVED_USER_INFO, Gson().toJson(response.value))
                }
                is ApiResponse.Error -> _error.postValue(ErrorWrapper(response.error, response.throwable))
            }
        }
    }

}