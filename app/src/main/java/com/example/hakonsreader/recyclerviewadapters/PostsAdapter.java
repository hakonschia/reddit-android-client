package com.example.hakonsreader.recyclerviewadapters;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.interfaces.OnClickListener;
import com.example.hakonsreader.views.ContentVideo;
import com.example.hakonsreader.views.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Adapter for recycler view of Reddit posts
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
    private static final String TAG = "PostsAdapter";
    

    private List<RedditPost> posts = new ArrayList<>();

    /**
     * Adds a list of posts to the current list of posts
     * <p>Duplicate posts are filtered out</p>
     *
     * @param newPosts The posts to add
     */
    public void addPosts(List<RedditPost> newPosts) {
        // The newly retrieved posts might include posts that have been "pushed down" by Reddit
        // so filter out any posts that are already in the list
        List<RedditPost> filtered = newPosts.stream()
                .filter(post -> {
                    for (RedditPost p : this.posts) {
                        // Filter out the post on matching ID
                        if (p.getID().equals(post.getID())) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        this.posts.addAll(filtered);
        notifyDataSetChanged();
    }

    /**
     * @return The list of posts in the adapter
     */
    public List<RedditPost> getPosts() {
        return posts;
    }


    @Override
    public int getItemCount() {
        return this.posts.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_item_post,
                parent,
                false
        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final RedditPost post = this.posts.get(position);

        // Don't show text posts here (only show when a post is opened)
        holder.post.setShowContent(post.getPostType() != PostType.TEXT);
        holder.post.setPostData(post);

        if (post.isMod()) {
            holder.asMod();
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.reset();
    }

    /**
     * The view for the items in the list
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private Post post;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            post = itemView.findViewById(R.id.post);
        }

        /**
         * Formats the post as a mod post
         */
        private void asMod() {
            post.asMod();
        }

        /**
         * Resets formatting to default
         */
        public void reset() {
            post.reset();
        }

        /**
         * Call when the view holder has been selected (ie. just been shown on the screen)
         *
         * <p>Plays a video if auto play is selected</p>
         */
        public void onSelected() {
            if (App.get().autoPlayVideos()) {
                post.playVideo();
            }
        }
    }
}
