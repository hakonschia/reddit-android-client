package com.example.hakonsreader.recyclerviewadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.views.VoteBar;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private List<RedditComment> comments = new ArrayList<>();


    public void addComments(List<RedditComment> comments) {
        this.comments.addAll(comments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_layout_comment,
                parent,
                false
        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RedditComment comment = this.comments.get(position);

        Instant created = Instant.ofEpochSecond(comment.getCreatedAt());
        Instant now = Instant.now();

        String time;

        Duration between = Duration.between(created, now);
        Context context = holder.itemView.getContext();

        // This is kinda bad but whatever
        if (between.toDays() > 0) {
            time = String.format(context.getString(R.string.post_age_days), between.toDays());
        } else if (between.toHours() > 0) {
            time = String.format(context.getString(R.string.post_age_hours), between.toHours());
        } else {
            time = String.format(context.getString(R.string.post_age_minutes), between.toMinutes());
        }

        String authorText = String.format(context.getString(R.string.authorPrefixed), comment.getAuthor());

        holder.content.setText("DEPTH: " + comment.getDepth() + "... " + comment.getBody());
        holder.author.setText(authorText);
        holder.age.setText(time);

        holder.voteBar.setListing(comment);
    }

    @Override
    public int getItemCount() {
        return this.comments.size();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView author;
        private TextView age;
        private TextView content;
        private VoteBar voteBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.author = itemView.findViewById(R.id.comment_author);
            this.age = itemView.findViewById(R.id.comment_age);
            this.content = itemView.findViewById(R.id.comment_content);
            this.voteBar = itemView.findViewById(R.id.comment_vote_bar);
        }
    }
}
