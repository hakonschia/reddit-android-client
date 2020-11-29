package com.example.hakonsreader.recyclerviewadapters.diffutils;

import androidx.recyclerview.widget.DiffUtil;

import com.example.hakonsreader.api.model.Subreddit;

import java.util.List;

/**
 * Callback class for DiffUtil for lists of {@link Subreddit}
 */
public class SubredditsDiffCallback extends DiffUtil.Callback {

    private final List<Subreddit> oldList;
    private final List<Subreddit> newList;

    public SubredditsDiffCallback(List<Subreddit> oldList, List<Subreddit> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }


    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Subreddit oldItem = oldList.get(oldItemPosition);
        Subreddit newItem = newList.get(newItemPosition);

        // We only have to compare what is actually shown in the list. If we used Subreddit.equals()
        // it would most likely return false, since subscribers will very likely be changed, causing
        // the list flash because this would return false and it would be redrawn
        return oldItem.getName().equals(newItem.getName())
                && oldItem.isFavorited() == newItem.isFavorited()
                && oldItem.getPublicDescription().equals(newItem.getPublicDescription());
    }
}