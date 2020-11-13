package com.example.hakonsreader.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.Subreddit;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for searching for subreddits
 */
public class SearchForSubredditsViewModel extends ViewModel {

    private final RedditApi api = App.get().getApi();
    private final MutableLiveData<List<Subreddit>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<ErrorWrapper> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> onCountChange = new MutableLiveData<>();


    /**
     * List of subreddits searched for
     *
     * @return A LiveData that can be observed to be notified of search results
     */
    public LiveData<List<Subreddit>> getSearchResults() {
        return searchResults;
    }

    /**
     * Errors received by the API
     *
     * @return A LiveData that can be observed to be notified when API errors occur
     */
    public LiveData<ErrorWrapper> getError() {
        return error;
    }

    public LiveData<Boolean> getOnCountChange() {
        return onCountChange;
    }

    /**
     * Searches for subreddits
     *
     * @param query The search query
     */
    public void search(String query) {
        onCountChange.postValue(true);
        api.subreddits().search(query, subreddits -> {
            onCountChange.postValue(false);
            searchResults.postValue(subreddits);
        }, (e, t) -> {
            onCountChange.postValue(false);
            error.postValue(new ErrorWrapper(e, t));
        });
    }

    /**
     * Clears the list of subreddits searched for
     */
    public void clearSearchResults() {
        searchResults.postValue(new ArrayList<>());
    }
}
