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

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.interfaces.OnClickListener;
import com.example.hakonsreader.interfaces.OnSubredditSelected;
import com.example.hakonsreader.misc.Util;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SubredditsAdapter extends RecyclerView.Adapter<SubredditsAdapter.ViewHolder> {
    private static final String TAG = "SubredditsAdapter";
    
    private OnSubredditSelected subredditSelected;
    private List<Subreddit> subreddits = new ArrayList<>();
    private OnClickListener<Subreddit> favoriteClicked;

    public void setSubredditSelected(OnSubredditSelected subredditSelected) {
        this.subredditSelected = subredditSelected;
    }

    public void setFavoriteClicked(OnClickListener<Subreddit> favoriteClicked) {
        this.favoriteClicked = favoriteClicked;
    }

    public void setSubreddits(List<Subreddit> subreddits) {
        this.subreddits = subreddits;
        notifyDataSetChanged();
    }

    /**
     * Called when a subreddit has been favorited/un-favorited
     *
     * <p>The function uses {@link Subreddit#userHasFavorited()} to calculate where to now
     * place the item in the list</p>
     *
     * @param subreddit The subreddit favorited/un-favorited
     */
    public void onFavorite(Subreddit subreddit) {
        // TODO find out where it actually should go
        int pos = subreddits.indexOf(subreddit);
        subreddits.remove(pos);
        subreddits.add(0, subreddit);

        notifyItemMoved(pos, 0);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Subreddit subreddit = subreddits.get(position);

        holder.name.setText(subreddit.getName());

        holder.updateFavorited(subreddit.userHasFavorited());

        String iconURL = subreddit.getIconImage();
        String communityURL = subreddit.getCommunityIcon();
        if (iconURL != null && !iconURL.isEmpty()) {
            Picasso.get()
                    .load(iconURL)
                    .into(holder.icon);
        } else if(communityURL != null && !communityURL.isEmpty()) {
            Picasso.get()
                    .load(communityURL)
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

            favorite.setOnClickListener(view -> {
                int pos = getAdapterPosition();

                if (favoriteClicked != null && pos != RecyclerView.NO_POSITION) {
                    favoriteClicked.onClick(subreddits.get(pos));
                }
            });
        }

        private void updateFavorited(boolean favorited) {
            if (favorited) {
                favorite.setColorFilter(ContextCompat.getColor(context, R.color.subredditFavorited));
            } else {
                favorite.setColorFilter(ContextCompat.getColor(context, R.color.iconColor));
            }
        }
    }
}
