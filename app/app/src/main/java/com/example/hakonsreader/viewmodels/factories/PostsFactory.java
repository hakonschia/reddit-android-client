package com.example.hakonsreader.viewmodels.factories;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hakonsreader.viewmodels.PostsViewModel;

public class PostsFactory implements ViewModelProvider.Factory {
    private final String userOrSubreddit;
    private final boolean isUser;

    /**
     * @param userOrSubreddit The name of the user or subreddit the ViewModel is for
     * @param isUser True if the ViewModel is for a user, false for a subreddit
     */
    public PostsFactory(String userOrSubreddit, boolean isUser) {
        this.userOrSubreddit = userOrSubreddit;
        this.isUser = isUser;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new PostsViewModel(userOrSubreddit, isUser);
    }
}
