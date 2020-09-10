package com.example.hakonsreader.recyclerviewadapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditComment;

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

        holder.content.setText("DEPTH: " + comment.getDepth() + "... " + comment.getBody());
    }

    @Override
    public int getItemCount() {
        return this.comments.size();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView content;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.content = itemView.findViewById(R.id.comment_content);
        }
    }
}
