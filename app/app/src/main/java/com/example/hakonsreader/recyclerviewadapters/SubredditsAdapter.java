package com.example.hakonsreader.recyclerviewadapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.interfaces.OnSubredditSelected;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SubredditsAdapter extends RecyclerView.Adapter<SubredditsAdapter.ViewHolder> {
    private static final String TAG = "SubredditsAdapter";
    
    private OnSubredditSelected subredditSelected;
    private List<Subreddit> subreddits = new ArrayList<>();

    public void setSubredditSelected(OnSubredditSelected subredditSelected) {
        this.subredditSelected = subredditSelected;
    }

    public void setSubreddits(List<Subreddit> subreddits) {
        this.subreddits = subreddits;
        notifyDataSetChanged();
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Subreddit subreddit = subreddits.get(position);

        holder.name.setText(subreddit.getName());

        holder.favorite.setOnClickListener(v -> {

        });
        if (subreddit.userHasFavorited()) {
            holder.favorite.setColorFilter(ContextCompat.getColor(holder.context, R.color.subredditFavorited));
        } else {
            holder.favorite.setColorFilter(ContextCompat.getColor(holder.context, R.color.iconColor));
        }

        String iconURL = subreddit.getIconImage();
        if (iconURL != null && !iconURL.isEmpty()) {
            Picasso.get()
                    .load(iconURL)
                    .into(holder.icon);
        } else {
            holder.icon.setImageDrawable(ContextCompat.getDrawable(holder.context, R.drawable.ic_baseline_emoji_emotions_24));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_item_subreddit,
                parent,
                false
        );
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return subreddits.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        private Context context;

        private ImageView icon;
        private TextView name;
        private ImageButton favorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();

            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            favorite = itemView.findViewById(R.id.favoriteSub);

            // Call the registered onClick listener when an item is clicked
            itemView.setOnClickListener(view -> {
                int pos = getAdapterPosition();

                if (subredditSelected != null && pos != RecyclerView.NO_POSITION) {
                    subredditSelected.subredditSelected(subreddits.get(pos));
                }
            });


            itemView.setOnLongClickListener(view -> {
                int pos = getAdapterPosition();

                if (subredditSelected != null && pos != RecyclerView.NO_POSITION) {
                    Subreddit subreddit = subreddits.get(pos);
                    
                    String k = String.format("{\n\tname:%s\n\ticonURL:%s\n\turl:%s\n}", subreddit.getName(), subreddit.getIconImage(), subreddit.getUrl());
                    Log.d(TAG, "onBindViewHolder: \n" +k);
                }
                return true;
            });
        }
    }
}
