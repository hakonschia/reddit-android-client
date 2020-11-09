package com.example.hakonsreader.recyclerviewadapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.views.ListDivider;
import com.example.hakonsreader.views.Post;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Adapter for recycler view of Reddit posts
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
    private static final String TAG = "PostsAdapter";
    
    private final List<RedditPost> posts = new ArrayList<>();

    /**
     * The amount of minutes scores should be hidden (default to -1 means not specified)
     */
    private int hideScoreTime = -1;


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
                    for (RedditPost p : posts) {
                        // Filter out the post on matching ID
                        if (p.getId().equals(post.getId())) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // If we use "notifyDataSetChanged" all the items get re-drawn, which means if a video is playing
        // it will be restarted. By using notifyItemRangeInserted the items on screen currently wont
        // be re-drawn
        int previousSize = posts.size();
        posts.addAll(filtered);
        notifyItemRangeInserted(previousSize, posts.size() - 1);
    }

    /**
     * Removes all posts from the list
     */
    public void clearPosts() {
        int size = posts.size();
        posts.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * @param hideScoreTime The amount of minutes scores should be hidden
     */
    public void setHideScoreTime(int hideScoreTime) {
        this.hideScoreTime = hideScoreTime;
    }

    /**
     * @return The list of posts in the adapter
     */
    public List<RedditPost> getPosts() {
        return posts;
    }


    @Override
    public int getItemCount() {
        return posts.size();
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
        final RedditPost post = posts.get(position);

        // Don't show text posts here (only show when a post is opened)
        holder.post.setShowContent(post.getPostType() != PostType.TEXT);

        // Disable ticker animation to avoid it updating when scrolling
        holder.post.enableTickerAnimation(false);

        Instant created = Instant.ofEpochSecond(post.getCreatedAt());
        Instant now = Instant.now();
        Duration between = Duration.between(created, now);
        holder.post.setHideScore(hideScoreTime > between.toMinutes());

        holder.post.setPostData(post);
        holder.post.enableTickerAnimation(true);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        ListDivider divider = new ListDivider(ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.list_divider));
        recyclerView.addItemDecoration(divider);
    }

    /**
     * The view for the items in the list
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Post post;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            post = itemView.findViewById(R.id.post);
        }

        /**
         * Call when the view holder has been selected (ie. it is now the main visible view holder)
         *
         * <p>Plays a video if auto play is selected</p>
         */
        public void onSelected() {
            if (App.get().autoPlayVideos()) {
                post.playVideo();
            }
        }

        /**
         * Call when the view holder has been unselected (ie. not the main visible view holder anymore)
         *
         * <p>If the view holder holds video content it is paused</p>
         */
        public void onUnSelected() {
            post.pauseVideo();
        }

        /**
         * Gets the position of the contents Y position on the screen
         * <p>Crossposts is taken into account and will return the position of the actual content
         * inside the crosspost</p>
         *
         * @return The Y position of the content
         */
        public int getContentY() {
            return post.getContentY();
        }

        /**
         * Gets the bottom position of the contents Y position on the screen
         * <p>Crossposts is taken into account and will return the position of the actual content
         * inside the crosspost</p>
         *
         * @return The Y position of the bottom of the content
         */
        public int getContentBottomY() {
            return post.getContentBottomY();
        }

        /**
         * Gets a bundle of extras that include the ViewHolder state
         *
         * <p>Use {@link PostsAdapter.ViewHolder#setExtras(Bundle)} to restore the state</p>
         *
         * @return The state of the ViewHolder
         */
        public Bundle getExtras() {
            return post.getExtras();
        }

        /**
         * Sets extras that have been saved to restore state.
         *
         * @param data The extras to set
         */
        public void setExtras(Bundle data) {
            post.setExtras(data);
        }

        /**
         * Call when the ViewHolder should be destroyed. Any resources are freed up
         */
        public void destroy() {
            post.cleanUpContent();
        }

        /**
         * Call when the view holding the ViewHolder has been paused. Any videos will be paused
         */
        public void pause() {
            post.pauseVideo();
        }
    }
}
