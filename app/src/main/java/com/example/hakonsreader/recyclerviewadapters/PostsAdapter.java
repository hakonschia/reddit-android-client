package com.example.hakonsreader.recyclerviewadapters;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.interfaces.OnClickListener;
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



        if (post.getPostHint().equals("image")) {
            Picasso.get().load(post.getUrl()).into(holder.contentImg);
        }


        holder.upvote.setOnClickListener(view -> this.upvote(post));
        holder.downvote.setOnClickListener(view -> this.downvote(post));

        // TODO onclicklisteners (check how I did it in hytte app)
    }

    @Override
    public int getItemCount() {
        return this.posts.size();
    }

    /**
     * Sends a request to upvote a given post
     *
     * @param post The post to upvote
     */
    private void upvote(RedditPost post) {
        if (this.redditApi == null) {
            return;
        }

        this.redditApi.vote(post.getId(), RedditApi.VoteType.Upvote, RedditApi.Thing.Post).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // TODO Change color of button
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }

    /**
     * Sends a request to upvote a given post
     *
     * @param post The post to upvote
     */
    private void downvote(RedditPost post) {
        if (this.redditApi == null) {
            return;
        }

        this.redditApi.vote(post.getId(), RedditApi.VoteType.Downvote, RedditApi.Thing.Post).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // TODO Change color of button
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView subreddit;
        private TextView author;
        private TextView title;
        private TextView score;
        private TextView comments;
        private ImageView upvote;
        private ImageView downvote;

        private FrameLayout content;
        private ImageView contentImg;


        private Resources resources;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.resources = itemView.getResources();

            this.subreddit = itemView.findViewById(R.id.listPostSubreddit);
            this.author = itemView.findViewById(R.id.listPostAuthor);
            this.title = itemView.findViewById(R.id.listPostTitle);
            this.score = itemView.findViewById(R.id.listPostScore);
            this.comments = itemView.findViewById(R.id.listPostComments);
            this.upvote = itemView.findViewById(R.id.listBtnUpvote);
            this.downvote = itemView.findViewById(R.id.listBtnDownvote);

            this.content = itemView.findViewById(R.id.listPostContent);
            this.contentImg = itemView.findViewById(R.id.listPostContentImg);

            // Call the registered onClick listener when an item is clicked
            itemView.setOnClickListener(view -> {
                int pos = getAdapterPosition();

                if (onClickListener != null && pos != RecyclerView.NO_POSITION) {
                    onClickListener.onClick(posts.get(pos));
                }
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
