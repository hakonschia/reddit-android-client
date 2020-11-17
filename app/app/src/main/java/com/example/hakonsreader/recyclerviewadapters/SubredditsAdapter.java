package com.example.hakonsreader.recyclerviewadapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.interfaces.OnClickListener;
import com.example.hakonsreader.interfaces.OnSubredditSelected;
import com.example.hakonsreader.views.util.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter for displaying a list of {@link Subreddit} items in a RecyclerView
 */
public class SubredditsAdapter extends RecyclerView.Adapter<SubredditsAdapter.ViewHolder> {
    private static final String TAG = "SubredditsAdapter";

    private List<Subreddit> subreddits = new ArrayList<>();
    private OnSubredditSelected subredditSelected;
    private OnClickListener<Subreddit> favoriteClicked;

    /**
     * Sets the listener for when a subreddit in the list has been clicked
     *
     * @param subredditSelected The callback to set
     */
    public void setSubredditSelected(OnSubredditSelected subredditSelected) {
        this.subredditSelected = subredditSelected;
    }

    /**
     * Sets the listener for when the "Favorite" icon has been clicked on an item in the list
     *
     * @param favoriteClicked The callback to set
     */
    public void setFavoriteClicked(OnClickListener<Subreddit> favoriteClicked) {
        this.favoriteClicked = favoriteClicked;
    }

    /**
     * Submit the list of subreddits to display
     *
     * @param newList The list of items to display
     */
    public void submitList(List<Subreddit> newList) {
        List<Subreddit> previous = this.subreddits;
        List<Subreddit> newSorted = sortSubreddits(newList);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new SubredditItemDiffCallback(previous, newSorted),
                true
        );

        this.subreddits = newSorted;
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Removes all subreddits from the list
     */
    public void clear() {
        int size = subreddits.size();
        subreddits.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Sorts a list of subreddits
     *
     * <p>The order of the list will be, sorted alphabetically:
     * <ol>
     *     <li>Favorites (for logged in users)</li>
     *     <li>The rest of the subreddits</li>
     *     <li>Users the user is following</li>
     * </ol>
     * </p>
     *
     * @param list The list so sort
     * @return A new list that is sorted based on the subreddit type
     */
    private List<Subreddit> sortSubreddits(List<Subreddit> list) {
        List<Subreddit> sorted = list.stream()
                // Sort based on subreddit name
                .sorted((first, second) -> first.getName().toLowerCase().compareTo(second.getName().toLowerCase()))
                .collect(Collectors.toList());

        List<Subreddit> favorites = sorted.stream()
                .filter(Subreddit::isFavorited)
                .collect(Collectors.toList());

        List<Subreddit> users = sorted.stream()
                .filter(subreddit -> subreddit.getSubredditType().equals("user"))
                .collect(Collectors.toList());

        // Remove the favorites to not include twice
        sorted.removeAll(favorites);
        sorted.removeAll(users);

        List<Subreddit> combined = new ArrayList<>();
        combined.addAll(favorites);
        combined.addAll(sorted);
        combined.addAll(users);

        return combined;
    }

    /**
     * Called when a subreddit has been favorited/un-favorited
     *
     * <p>The function uses {@link Subreddit#isFavorited()} to calculate where to now
     * place the item in the list</p>
     *
     * @param subreddit The subreddit favorited/un-favorited
     */
    public void onFavorite(Subreddit subreddit) {
        int pos = subreddits.indexOf(subreddit);
        subreddits.remove(pos);

        int newPos = findPosForItem(subreddit);

        subreddits.add(newPos, subreddit);

        // itemMoved just moves the item, itemChanged updates the view
        notifyItemMoved(pos, newPos);
        notifyItemChanged(newPos);
    }

    /**
     * Finds the position of where an item should be inserted in the list based on if it is favorited
     * or not
     *
     * @param subreddit The subreddit to find the index for
     * @return The index the item should be inserted
     */
    private int findPosForItem(Subreddit subreddit) {
        int posFirstNonFavorite = 0;
        for (int i = 0; i < subreddits.size(); i++) {
            Subreddit s = subreddits.get(i);
            if (!s.isFavorited()) {
                posFirstNonFavorite = i;
                break;
            }
        }

        // Find a sublist of where the item should go (favorite or not)
        List<Subreddit> list;
        // Subreddit has been favorited, get the sublist of favorites
        if (subreddit.isFavorited()) {
            list = subreddits.subList(0, posFirstNonFavorite);
        } else {
            // Sublist of everything not favorited
            list = subreddits.subList(posFirstNonFavorite, subreddits.size());
        }

        // binarySearch() returns the index of the item, or a negative representing where it would have been
        // The list will always be sorted on the name
        int newPos = Collections.binarySearch(list, subreddit, (s1, s2) -> s1.getName().toLowerCase().compareTo(s2.getName().toLowerCase()));

        // Add one to the value and invert it to get the actual position
        // ie. newPos = -5 means it would be in position 5 (index 4)
        newPos++;
        newPos *= -1;

        // The pos is in the sublist, so if we're unfavoriting we need to add the favorites size
        if (!subreddit.isFavorited()) {
            newPos += posFirstNonFavorite;
        }

        return newPos;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Subreddit subreddit = subreddits.get(position);

        holder.name.setText(subreddit.getName());
        App.get().getMark().setMarkdown(holder.description, subreddit.getPublicDesription());
        ViewUtil.setSubredditIcon(holder.icon, subreddit);

        if (subreddit.isSubscribed()) {
            holder.favorite.setVisibility(View.VISIBLE);
            holder.updateFavorited(subreddit.isFavorited());
        } else {
            holder.favorite.setVisibility(View.GONE);
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
        private TextView description;
        private ImageButton favorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();

            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            description = itemView.findViewById(R.id.subredditDescription);
            favorite = itemView.findViewById(R.id.favoriteSub);

            // TODO switching orientation somehow disables the listener? subredditSelected is probably null?
            // Call the registered onClick listener when an item is clicked
            itemView.setOnClickListener(view -> {
                int pos = getAdapterPosition();

                if (subredditSelected != null && pos != RecyclerView.NO_POSITION) {
                    subredditSelected.subredditSelected(subreddits.get(pos).getName());
                }
            });
            description.setOnClickListener(view -> {
                int pos = getAdapterPosition();
                TextView tv = (TextView) view;

                // Not a hyperlink (even long clicking on the hyperlink would open it, so don't collapse as well)
                if (tv.getSelectionStart() == -1 && tv.getSelectionEnd() == -1
                        && subredditSelected != null && pos != RecyclerView.NO_POSITION) {
                    subredditSelected.subredditSelected(subreddits.get(pos).getName());
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


    /**
     * Callback class for DiffUtil
     */
    private static class SubredditItemDiffCallback extends DiffUtil.Callback {

        private final List<Subreddit> oldList;
        private final List<Subreddit> newList;

        public SubredditItemDiffCallback(List<Subreddit> oldList, List<Subreddit> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }


        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Subreddit oldItem = oldList.get(oldItemPosition);
            Subreddit newItem = newList.get(newItemPosition);

            // We only have to compare what is actually shown in the list. If we used Subreddit.equals()
            // it would most likely return false, since subscribers will very likely be changed, causing
            // the list flash because this would return false and it would be redrawn
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.isFavorited() == newItem.isFavorited()
                    && oldItem.getPublicDesription().equals(newItem.getPublicDesription());
        }
    }
}
