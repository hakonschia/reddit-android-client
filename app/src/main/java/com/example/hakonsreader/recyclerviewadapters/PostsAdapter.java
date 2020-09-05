package com.example.hakonsreader.recyclerviewadapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.MainActivity;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditPost.PostType;
import com.example.hakonsreader.interfaces.OnClickListener;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Adapter for recycler view of Reddit posts
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
    private static final String TAG = "PostsAdapter";
    

    private List<RedditPost> posts = new ArrayList<>();
    private RedditApi redditApi = null;

    // Listener for when a list item has been clicked
    private OnClickListener<RedditPost> onClickListener;

    private OnClickListener<RedditPost> onLongClickListener;

    private OnClickListener<String> onSubredditClickListener;


    /**
     * Sets the RedditApi object to use for API calls
     *
     * @param api The API object to use
     */
    public void setRedditApi(@Nullable RedditApi api) {
        this.redditApi = api;
    }

    /**
     * Sets the click listener for when an item in the list has been clicked
     *
     * @param onClickListener The listener :)
     */
    public void setOnClickListener(OnClickListener<RedditPost> onClickListener) {
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
     * Sets the click listener for when the subreddit text on an item has been clicked
     *
     * @param onSubredditClickListener The listener :)
     */
    public void setOnSubredditClickListener(OnClickListener<String> onSubredditClickListener) {
        this.onSubredditClickListener = onSubredditClickListener;
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

    public List<RedditPost> getPosts() {
        return posts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.post_layout,
                parent,
                false
        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final RedditPost post = this.posts.get(position);

        String subreddit = String.format(holder.resources.getString(R.string.subredditPrefixed), post.getSubreddit());
        String author = String.format(holder.resources.getString(R.string.authorPrefixed), post.getAuthor());
        String numComments = holder.resources.getQuantityString(R.plurals.numComments, post.getAmountOfComments(), post.getAmountOfComments());

        holder.subreddit.setText(subreddit);
        holder.author.setText(author);
        holder.title.setText(post.getTitle());
        holder.score.setText(String.format("%d", post.getScore()));
        holder.comments.setText(numComments);

        holder.upvote.setOnClickListener(v -> this.vote(post, RedditApi.VoteType.Upvote));
        holder.downvote.setOnClickListener(v -> this.vote(post, RedditApi.VoteType.Downvote));

        this.updateVoteButtonColors(post, holder);
        this.addPostContent(post, holder);
    }

    private void updateVoteButtonColors(RedditPost post, ViewHolder holder) {
        RedditApi.VoteType voteType = post.getVoteType();

        // TODO no vote cast (api returns null which gson interprets as false :))


        int color;
        Context context = holder.itemView.getContext();

        switch (voteType) {
            case Upvote:
                color = R.color.upvoted;
                holder.upvote.setBackgroundTintList(ContextCompat.getColorStateList(context, color));
                break;

            case Downvote:
                color = R.color.downvoted;
                holder.downvote.setBackgroundTintList(ContextCompat.getColorStateList(context, color));
                break;

            case NoVote:
            default:
                color = R.color.textColor;
                holder.upvote.setBackgroundTintList(null);
                holder.downvote.setBackgroundTintList(null);
                break;
        }

        holder.score.setTextColor(context.getColor(color));
    }

    private void addPostContent(RedditPost post, ViewHolder holder) {
        // Add the content
        View view = null;

        PostType postType = post.getPostType();

        switch (postType) {
            case Image:
                // TODO when clicked open the image so you can ZOOOOOM
                ImageView imageView = new ImageView(holder.itemView.getContext());

                Picasso.get()
                        .load(post.getUrl())
                        .placeholder(R.drawable.ic_baseline_wifi_tethering_150)
                        // Scale so the image fits the width of the screen
                        .resize(MainActivity.SCREEN_WIDTH, 0)
                        .into(imageView);

                view = imageView;
                break;

            case Video:
                VideoView videoView = new VideoView(holder.itemView.getContext());
                videoView.setVideoPath(post.getVideoUrl());

                videoView.start();
                view = videoView;
                break;

            case RichVideo:
                // Links such as youtube, gfycat etc are rich video posts
                break;

            case Link:
                String url = post.getUrl();

                TextView textView = new TextView(holder.itemView.getContext());
                textView.setText(url);
                // TODO fix this to add a theme
                textView.setTextColor(holder.resources.getColor(R.color.linkColor));
                textView.setOnClickListener(v -> {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    holder.itemView.getContext().startActivity(i);
                });

                view = textView;
                break;

            case Text:
                // Do nothing special for text posts
                break;
        }

        // Since the ViewHolder is recycled it can still have views from other posts
        holder.content.removeAllViewsInLayout();
        // Make sure the view size resets (or it will still have the previous view size)
        holder.content.forceLayout();

        // A view to add (not text post)
        if (view != null) {
            holder.content.addView(view);
        }
    }

    @Override
    public int getItemCount() {
        return this.posts.size();
    }

    /**
     * Sends a request to vote on a given post
     *
     * @param post The post to upvote
     * @param voteType The way to vote. If this vote is already what is voted the request is changed
     *                 to VoteType.Unvote
     */
    private void vote(RedditPost post, RedditApi.VoteType voteType) {
        if (this.redditApi == null) {
            return;
        }

        // Ie. if upvote is clicked when the post is already upvoted, unvote the post
        if (voteType == post.getVoteType()) {
            voteType = RedditApi.VoteType.NoVote;
        }

        RedditApi.VoteType finalVoteType = voteType;
        this.redditApi.vote(post.getId(), voteType, RedditApi.Thing.Post).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                     post.setVoteType(finalVoteType);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView subreddit;
        private TextView author;
        private TextView title;
        private TextView score;
        private TextView comments;
        private ImageView upvote;
        private ImageView downvote;

        private FrameLayout content;

        private View itemView;
        private Resources resources;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;

            this.resources = itemView.getResources();

            this.subreddit = itemView.findViewById(R.id.listPostSubreddit);
            this.author = itemView.findViewById(R.id.listPostAuthor);
            this.title = itemView.findViewById(R.id.listPostTitle);
            this.score = itemView.findViewById(R.id.listPostScore);
            this.comments = itemView.findViewById(R.id.listPostComments);
            this.upvote = itemView.findViewById(R.id.listBtnUpvote);
            this.downvote = itemView.findViewById(R.id.listBtnDownvote);

            this.content = itemView.findViewById(R.id.listPostContent);

            Log.d(TAG, "ViewHolder: viewholder created");
            // Call the registered onClick listener when an item is clicked
            itemView.setOnClickListener(view -> {
                int pos = getAdapterPosition();
                String data = new GsonBuilder().setPrettyPrinting().create().toJson(posts.get(pos));
                Log.d(TAG, "ViewHolder: " + data);
                Log.d(TAG, "ViewHolder: " + posts.get(pos).getPostType());
                
                if (onClickListener != null && pos != RecyclerView.NO_POSITION) {
                    onClickListener.onClick(posts.get(pos));
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

            // Call the registered listener for when the text is clicked
            this.subreddit.setOnClickListener(view -> {
                int pos = getAdapterPosition();

                if (onSubredditClickListener != null && pos != RecyclerView.NO_POSITION) {
                    onSubredditClickListener.onClick(posts.get(pos).getSubreddit());
                }
            });
        }
    }
}
