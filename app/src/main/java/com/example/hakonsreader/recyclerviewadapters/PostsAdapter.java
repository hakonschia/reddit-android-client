package com.example.hakonsreader.recyclerviewadapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.flairs.RichtextFlair;
import com.example.hakonsreader.interfaces.OnClickListener;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.misc.ViewUtil;
import com.example.hakonsreader.views.FullPostBar;
import com.example.hakonsreader.views.ContentLink;
import com.example.hakonsreader.views.ContentVideo;
import com.example.hakonsreader.views.PostInfo;
import com.example.hakonsreader.views.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Adapter for recycler view of Reddit posts
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
    private static final String TAG = "PostsAdapter";
    

    private List<RedditPost> posts = new ArrayList<>();

    // Listener for when a list item has been clicked
    private OnClickListener<ViewHolder> onClickListener;

    // Listener for when a list item has been long clicked
    private OnClickListener<RedditPost> onLongClickListener;


    /**
     * Sets the click listener for when an item in the list has been clicked
     *
     * @param onClickListener The listener :)
     */
    public void setOnClickListener(OnClickListener<ViewHolder> onClickListener) {
        this.onClickListener = onClickListener;
    }

    /**
     * Sets the listener for long clicks
     *
     * @param onLongClickListener The listener :-)
     */
    public void setOnLongClickListener(OnClickListener<RedditPost> onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }


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
        Context context = holder.itemView.getContext();
        final RedditPost post = this.posts.get(position);
        holder.post = post;

        holder.postInfo.setPost(post);
        holder.fullPostBar.setPost(post);

        if (post.isSpoiler()) {
            holder.tags.addView(ViewUtil.createSpoilerTag(context));
        }
        if (post.isNSFW()) {
            holder.tags.addView(ViewUtil.createNSFWTag(context));
        }

        List<RichtextFlair> flairs = post.getLinkRichtextFlairs();
        flairs.forEach(flair -> {
            Tag tag = new Tag(context);
            tag.setText(flair.getText());
            // TODO this shouldn't be hardcoded like this
            // TODO each flair item is different types of items in the flair (such as an icon and text)
            

            if (post.getLinkFlairTextColor().equals("dark")) {
                tag.setTextColor(ContextCompat.getColor(context, R.color.flairTextDark));
                tag.setFillColor(ContextCompat.getColor(context, R.color.flairBackgroundDark));
            }
           // tag.setFillColor(post.getLinkFlairBackgroundColor());
            holder.tags.addView(tag);
        });

        // Update to set the initial vote status
        holder.setPostContent();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);

        // Free up any resources that might not be garbage collected automatically
        View v = holder.content.getChildAt(0);
        Util.cleanupPostContent(v);
        holder.tags.removeAllViews();

        // Remove previous post content
        holder.content.removeAllViewsInLayout();
        // Make sure the view size resets (or it will still have size of the previous post in this view holder)
        holder.content.forceLayout();
    }


    /**
     * The view for the items in the list
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private RedditPost post;

        private PostInfo postInfo;
        private LinearLayout tags;
        private FullPostBar fullPostBar;

        private FrameLayout content;

        private View itemView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;

            this.postInfo = itemView.findViewById(R.id.postInfo);
            this.tags = itemView.findViewById(R.id.tags);
            this.fullPostBar = itemView.findViewById(R.id.postFullBar);

            this.content = itemView.findViewById(R.id.content);

            // Call the registered onClick listener when an item is clicked
            itemView.setOnClickListener(view -> {
                int pos = getAdapterPosition();

                if (onClickListener != null && pos != RecyclerView.NO_POSITION) {
                    onClickListener.onClick(this);
                }
            });

            // Call the registered onClick listener when an item is long clicked
            itemView.setOnLongClickListener(view -> {
                int pos = getAdapterPosition();

                if (onLongClickListener != null && pos != RecyclerView.NO_POSITION) {
                    onLongClickListener.onClick(posts.get(pos));

                    return true;
                }

                return false;
            });
        }

        /**
         * Gets a list of pairs of the views that should be transitioned to the post activity
         *
         * @return An array of {@link Pair<View, String>} mapping the view to its transition name in the new activity
         */
        public Pair<View, String>[] getPostTransitionViews() {
           return new Pair[] {
                Pair.create(this.postInfo, "post_info"),
                Pair.create(this.fullPostBar, "post_full_bar"),
                Pair.create(this.content, "post_content")
            };
        }

        /**
         * The post this view holder currently holds
         *
         * @return The Reddit post shown in this view holder
         */
        public RedditPost getPost() {
            return post;
        }

        /**
         * Retrieves any extra information about the content of the post
         *
         * <p>Currently only returns the timestamp of the video, if the post is a video post</p>
         * @return A bundle that might include extra information about the state of the post
         */
        public Bundle getExtraPostInfo() {
            Bundle bundle = new Bundle();

            View c = content.getChildAt(0);
            if (c instanceof ContentVideo) {
                ContentVideo video = (ContentVideo)c;
                bundle.putLong(ContentVideo.EXTRA_TIMESTAMP, video.getCurrentPosition());
                bundle.putBoolean(ContentVideo.EXTRA_IS_PLAYING, video.isPlaying());
                bundle.putBoolean(ContentVideo.EXTRA_SHOW_CONTROLS, video.isControllerShown());
            }

            return bundle;
        }

        /**
         * Sets the content of the post
         */
        private void setPostContent() {
            View view = Util.generatePostContent(post, itemView.getContext());

            // A view to add and not text post (don't add text posts to the list)
            if (view != null && post.getPostType() != PostType.TEXT) {
                content.addView(view);
            }

            // TODO this should be done somewhere else to not have the same code here and in PostActivity
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) content.getLayoutParams();
            // Align link post to start of parent
            // TODO make this not so bad
            if (view instanceof ContentLink) {
                params.removeRule(RelativeLayout.CENTER_IN_PARENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
            } else {
                params.removeRule(RelativeLayout.ALIGN_PARENT_START);
                params.addRule(RelativeLayout.CENTER_IN_PARENT);
            }

            content.setLayoutParams(params);
        }
    }
}
