package com.example.hakonsreader.recyclerviewadapters;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link RedditPost}.
 */
public class SubredditRecyclerViewAdapter extends RecyclerView.Adapter<SubredditRecyclerViewAdapter.ViewHolder> {

    private final List<RedditPost> posts;

    public SubredditRecyclerViewAdapter(List<RedditPost> items) {
        posts = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.post_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.item = posts.get(position);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public RedditPost item;
        public TextView postTitle;

        public ViewHolder(View view) {
            super(view);
            this.view = view;

            postTitle = view.findViewById(R.id.postTitle);
            postTitle.setText(item.getTitle());
        }
    }
}