package com.example.hakonsreader.recyclerviewadapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.interfaces.OnClickListener;
import com.example.hakonsreader.recyclerviewadapters.diffutils.PostsDiffCallback;
import com.example.hakonsreader.views.ListDivider;
import com.example.hakonsreader.views.Post;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


/**
 * Adapter for recycler view of Reddit posts
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
    private static final String TAG = "PostsAdapter";
    
    private List<RedditPost> posts = new ArrayList<>();

    /**
     * The amount of minutes scores should be hidden (default to -1 means not specified)
     */
    private int hideScoreTime = -1;


    /**
     * Submits the list of posts to show in the RecyclerView
     *
     * @param newPosts The posts to show
     */
    public void submitList(List<RedditPost> newPosts) {
        // If there are no posts we don't have to diff the posts as they will all be gone
        if (newPosts.isEmpty()) {
            clearPosts();
        } else {
            List<RedditPost> previous = posts;

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new PostsDiffCallback(previous, newPosts)
            );

            posts = newPosts;
            diffResult.dispatchUpdatesTo(this);
        }
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

    /**
     * Formats the author text based on whether or not it is posted by a a mod or admin
     * If no match is found, the default author color is used
     *
     * <p>If multiple values are true, the precedence is:
     * <ol>
     *     <li>Admin</li>
     *     <li>Mod</li>
     * </ol>
     * </p>
     *
     * @param tv The TextView to format
     * @param post The post the text is for
     */
    @BindingAdapter("authorTextColorPost")
    public static void formatAuthor(TextView tv, RedditPost post) {
        if (post == null) {
            return;
        }

        tv.setTypeface(Typeface.DEFAULT);
        if (post.isAdmin()) {
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.commentByAdminBackground));
        } else if (post.isMod()) {
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.commentByModBackground));
        } else {
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.link_color));
        }
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
        holder.post.setShowTextContent(false);

        // Disable ticker animation to avoid it updating when scrolling
        holder.post.enableTickerAnimation(false);

        Instant created = Instant.ofEpochSecond(post.getCreatedAt());
        Instant now = Instant.now();
        Duration between = Duration.between(created, now);
        holder.post.setHideScore(hideScoreTime > between.toMinutes());

        holder.post.setRedditPost(post);
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
         */
        public void onSelected() {
            post.viewSelected();
        }

        /**
         * Call when the view holder has been unselected (ie. not the main visible view holder anymore)
         */
        public void onUnselected() {
            post.viewUnselected();
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
    }
}
