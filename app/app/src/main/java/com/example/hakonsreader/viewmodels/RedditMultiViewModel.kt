package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditMulti
import com.example.hakonsreader.api.responses.ApiResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

/**
 * ViewModel for retrieving a list of a users [RedditMulti]
 */
class RedditMultiViewModel @AssistedInject constructor(
        private val api: RedditApi,
        @Assisted var username: String?
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        /**
         * Factory for the ViewModel
         *
         * @param username The username of the user to retrieve multis from, or null if for the logged in user
         */
        fun create(
                username: String?
        ): RedditMultiViewModel
    }

    private var loadedFromApi = false

    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()
    private val _multis = MutableLiveData<List<RedditMulti>>()

    val multis: LiveData<List<RedditMulti>> = _multis
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error

    /**
     * Load multis. If a user is logged in their own multis subreddits are loaded, otherwise
     * multis for the given username are loaded
     *
     * @param force If true then multis will be forced to load, even if previously loaded
     */
    fun loadMultis(force: Boolean = false) {
        if (loadedFromApi && !force) {
            return
        }
        _isLoading.value = true

        viewModelScope.launch {
            val response = if (username != null) {
                api.user(username!!).multis()
            } else {
                api.user().multis()
            }
            _isLoading.postValue(false)

            when (response) {
                is ApiResponse.Success -> {
                    loadedFromApi = true
                    _multis.postValue(response.value!!)
                }
                is ApiResponse.Error -> {
                    _error.postValue(ErrorWrapper(response.error, response.throwable))
                }
            }
        }
    }
}