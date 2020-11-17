package com.example.hakonsreader.recyclerviewadapters.diffutils;

import androidx.recyclerview.widget.DiffUtil;

import com.example.hakonsreader.api.model.RedditComment;

import java.util.List;
import java.util.Objects;

/**
 * Callback class for DiffUtil for lists of {@link RedditComment}
 */
public class CommentsDiffCallback extends DiffUtil.Callback {

    private final List<RedditComment> oldList;
    private final List<RedditComment> newList;

    public CommentsDiffCallback(List<RedditComment> oldList, List<RedditComment> newList) {
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
        RedditComment oldItem = oldList.get(oldItemPosition);
        RedditComment newItem = newList.get(newItemPosition);

        // When new comments are loaded the first comment will have the same ID as the
        // "more comments" comment, so we also have to check if the kind is the same
        return oldItem.getId().equals(newItem.getId())
                && oldItem.getKind().equals(newItem.getKind());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        RedditComment oldItem = oldList.get(oldItemPosition);
        RedditComment newItem = newList.get(newItemPosition);

        // There can be a mismatch here with "2 more comments" and normal comments, and these values
        // are null for "2 more comments" so use Objects.equals
        return Objects.equals(oldItem.getAuthor(), newItem.getAuthor()) &&
                Objects.equals(oldItem.getBody(), newItem.getBody());
    }
}
