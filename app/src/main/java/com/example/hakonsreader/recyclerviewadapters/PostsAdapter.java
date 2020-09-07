package com.example.hakonsreader.recyclerviewadapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.MainActivity;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditPost.PostType;
import com.example.hakonsreader.interfaces.OnClickListener;
import com.example.hakonsreader.views.PostInfo;
import com.example.hakonsreader.views.VoteBar;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


/**
 * Adapter for recycler view of Reddit posts
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
    private static final String TAG = "PostsAdapter";
    

    private List<RedditPost> posts = new ArrayList<>();
    private RedditApi redditApi = RedditApi.getInstance();

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
     *
     * @param posts The posts to add
     */
    public void addPosts(List<RedditPost> posts) {
        this.posts.addAll(posts);
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
                R.layout.list_layout_post,
                parent,
                false
        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final RedditPost post = this.posts.get(position);
        holder.post = post;

        String numComments = holder.resources.getQuantityString(R.plurals.numComments, post.getAmountOfComments(), post.getAmountOfComments());
        holder.comments.setText(numComments);

        holder.postInfo.setPost(post);
        holder.voteBar.setPost(post);

        // Update to set the initial vote status
        holder.setPostContent();
    }


    /**
     * The view for the items in the list
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private RedditPost post;

        private TextView comments;

        private PostInfo postInfo;
        private VoteBar voteBar;

        private View postFullBar;

        private FrameLayout content;

        private View itemView;
        private Resources resources;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.resources = itemView.getResources();

            this.postInfo = itemView.findViewById(R.id.post_info);
            this.postFullBar = itemView.findViewById(R.id.post_full_bar);

            this.comments = itemView.findViewById(R.id.post_comments);

            this.voteBar = itemView.findViewById(R.id.vote_bar);

            this.content = itemView.findViewById(R.id.post_content);

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
            // TODO possibly add content
           return new Pair[] {
                Pair.create(this.postInfo, "post_info"),
                Pair.create(this.postFullBar, "post_full_bar")
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
         * Sets the content of the post
         */
        private void setPostContent() {
            //Log.d(TAG, "addPostContent: " + new GsonBuilder().setPrettyPrinting().create().toJson(post));

            // Add the content
            View view = null;

            PostType postType = post.getPostType();

            switch (postType) {
                case Image:
                    view = this.generateImageContent(post);
                    break;

                case Video:
                    view = this.generateVideoContent(post);
                    break;

                case RichVideo:
                    // Links such as youtube, gfycat etc are rich video posts
                    break;

                case Link:
                    view = this.generateLinkContent(post);
                    break;

                case Text:
                    // Do nothing special for text posts
                    break;
            }

            // Since the ViewHolder is recycled it can still have views from other posts
            content.removeAllViewsInLayout();
            // Make sure the view size resets (or it will still have size of the previous post in this view holder)
            content.forceLayout();

            // A view to add (not text post)
            if (view != null) {
                content.addView(view);
            }
        }

        /**
         * Generates the content for image posts
         *
         * @param post The post to generate content for
         * @return An ImageView with the image of the post set to match the screen width
         */
        private ImageView generateImageContent(RedditPost post) {
            // TODO when clicked open the image so you can ZOOOOOM
            ImageView imageView = new ImageView(itemView.getContext());

            Picasso.get()
                    .load(post.getUrl())
                    .placeholder(R.drawable.ic_baseline_wifi_tethering_150)
                    // Scale so the image fits the width of the screen
                    .resize(MainActivity.SCREEN_WIDTH, 0)
                    .into(imageView);

            return imageView;
        }

        /**
         * Generates the content for video posts
         *
         * @param post The post to generate content for
         * @return A VideoView
         */
        private VideoView generateVideoContent(RedditPost post) {
            VideoView videoView = new VideoView(itemView.getContext());
            //videoView.setVideoPath(post.getVideoUrl());

           // videoView.start();
            return videoView;
        }

        /**
         * Generates the content for link posts
         *
         * @param post The post to generate content for
         * @return A TextView
         */
        private TextView generateLinkContent(RedditPost post) {
            String url = post.getUrl();

            Context context = itemView.getContext();

            TextView textView = new TextView(context);
            textView.setText(url);
            textView.setTextColor(context.getColor(R.color.linkColor));
            textView.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                itemView.getContext().startActivity(i);
            });

            return textView;
        }
    }
}
