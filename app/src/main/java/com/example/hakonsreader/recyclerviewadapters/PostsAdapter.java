package com.example.hakonsreader.recyclerviewadapters;

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

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    private List<RedditPost> posts = new ArrayList<>();

    public void setPosts(List<RedditPost> posts) {
        this.posts = posts;
        notifyDataSetChanged();
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

        holder.title.setText(post.getTitle());

        // TODO move to string literal with placeholder
        holder.subreddit.setText("r/" + post.getSubreddit());
        holder.comments.setText(post.getAmountOfComments() + " comments");

        // TODO onclicklisteners (check how I did it in hytte app)
    }

    @Override
    public int getItemCount() {
        return this.posts.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView title;
        private TextView subreddit;
        private TextView comments;
        private Button upvote;
        private Button downvote;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.title = itemView.findViewById(R.id.postTitle);
            this.subreddit = itemView.findViewById(R.id.postSubreddit);
            this.comments = itemView.findViewById(R.id.postComments);
            this.upvote = itemView.findViewById(R.id.btnUpvote);
            this.downvote = itemView.findViewById(R.id.btnDownvote);
        }
    }
}
