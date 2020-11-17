package com.example.hakonsreader.recyclerviewadapters.diffutils;

import androidx.recyclerview.widget.DiffUtil;

import com.example.hakonsreader.api.model.RedditPost;

import java.util.List;

/**
 * Callback class for DiffUtil for lists of {@link RedditPost}
 */
public class PostsDiffCallback extends DiffUtil.Callback {

    private final List<RedditPost> oldPosts;
    private final List<RedditPost> newPosts;

    public PostsDiffCallback(List<RedditPost> oldPosts, List<RedditPost> newPosts) {
        this.oldPosts = oldPosts;
        this.newPosts = newPosts;
    }


    @Override
    public int getOldListSize() {
        return oldPosts.size();
    }

    @Override
    public int getNewListSize() {
        return newPosts.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldPosts.get(oldItemPosition).getId().equals(newPosts.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return false;
    }
}
