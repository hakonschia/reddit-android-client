package com.example.hakonsreader.viewmodels.factories;

import android.content.Context;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hakonsreader.viewmodels.PostsViewModel;

/**
 * Factory for PostsViewModel's
 */
public class PostsFactory implements ViewModelProvider.Factory {
    private final Context context;
    private final String userOrSubreddit;
    private final boolean isUser;

    /**
     * @param context The context for creating the ViewModel
     * @param userOrSubreddit The name of the user or subreddit the ViewModel is for
     * @param isUser True if the ViewModel is for a user, false for a subreddit
     */
    public PostsFactory(Context context, String userOrSubreddit, boolean isUser) {
        this.context = context;
        this.userOrSubreddit = userOrSubreddit;
        this.isUser = isUser;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new PostsViewModel(context, userOrSubreddit, isUser);
    }
}
