package com.example.hakonsreader.recyclerviewadapters;

import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;

import java.util.ArrayList;
import java.util.List;


/**
 * Adapter for recycler view of Reddit posts
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
    private static final String TAG = "PostsAdapter";
    

    private List<RedditPost> posts = new ArrayList<>();


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
        RedditPost post = this.posts.get(position);

        String subreddit = String.format(holder.resources.getString(R.string.subredditPrefixed), post.getSubreddit());
        String author = String.format(holder.resources.getString(R.string.authorPrefixed), post.getAuthor());
        String numComments = holder.resources.getQuantityString(R.plurals.numComments, post.getAmountOfComments(), post.getAmountOfComments());

        holder.subreddit.setText(subreddit);
        holder.author.setText(author);
        holder.title.setText(post.getTitle());
        holder.score.setText(String.format("%d", post.getScore()));
        holder.comments.setText(numComments);

        // TODO onclicklisteners (check how I did it in hytte app)
    }

    @Override
    public int getItemCount() {
        return this.posts.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView subreddit;
        private TextView author;
        private TextView title;
        private TextView score;
        private TextView comments;
        private Button upvote;
        private Button downvote;

        private Resources resources;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.resources = itemView.getResources();

            this.subreddit = itemView.findViewById(R.id.postSubreddit);
            this.author = itemView.findViewById(R.id.postAuthor);
            this.title = itemView.findViewById(R.id.postTitle);
            this.score = itemView.findViewById(R.id.postScore);
            this.comments = itemView.findViewById(R.id.postComments);
            this.upvote = itemView.findViewById(R.id.btnUpvote);
            this.downvote = itemView.findViewById(R.id.btnDownvote);
        }
    }
}
