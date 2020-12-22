package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.databinding.ListItemSubredditBinding
import com.example.hakonsreader.interfaces.OnClickListener
import com.example.hakonsreader.interfaces.OnSubredditSelected
import com.example.hakonsreader.recyclerviewadapters.diffutils.SubredditsDiffCallback
import com.example.hakonsreader.views.util.ViewUtil
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

/**
 * Adapter for displaying a list of [Subreddit] items
 */
class SubredditsAdapter : RecyclerView.Adapter<SubredditsAdapter.ViewHolder>() {

    private var subreddits: MutableList<Subreddit> = ArrayList()

    /**
     * The listener for when a subreddit in the list has been clicked
     */
    var subredditSelected: OnSubredditSelected? = null

    /**
     * The listener for when the "Favorite" icon in a list item has been clicked
     */
    var favoriteClicked: OnClickListener<Subreddit>? = null


    /**
     * Submit the list of subreddits to display
     *
     * @param list The list of items to display
     * @param sort If set to true the list will be sorted in the following order:
     *          <ol>
     *              <li>Favorites (for logged in users)</li>
     *               <li>The rest of the subreddits</li>
     *               <li>Users the user is following</li>
     *          </ol>
     */
    fun submitList(list: MutableList<Subreddit>, sort: Boolean) {
        val previous = subreddits

        val sorted = if (sort) {
            sortSubreddits(list)
        } else {
            list
        }

        val diffResults = DiffUtil.calculateDiff(
                SubredditsDiffCallback(previous, sorted),
                true
        )

        subreddits = sorted
        diffResults.dispatchUpdatesTo(this)
    }

    /**
     * Clears the items in the adapter
     */
    fun clear() {
        val size = subreddits.size
        subreddits.clear()
        notifyItemRangeRemoved(0, size)
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
    private fun sortSubreddits(list: List<Subreddit>) : MutableList<Subreddit> {
        val sorted = list.stream()
                .sorted { o1, o2 ->  o1.name.toLowerCase().compareTo(o2.name.toLowerCase())}
                .collect(Collectors.toList())

        val favorites = sorted.stream()
                .filter { s -> s.isFavorited }
                .collect(Collectors.toList())

        val users = sorted.stream()
                .filter { s -> s.subredditType == "user"}
                .collect(Collectors.toList())

        // Remove favorites and users so they aren't included twice
        sorted.removeAll(favorites)
        sorted.removeAll(users)

        val combined = ArrayList<Subreddit>()
        combined.addAll(favorites)
        combined.addAll(sorted)
        combined.addAll(users)

        return combined
    }

    /**
     * Updates the position and view for a subreddit based on [Subreddit.isFavorited]
     *
     * @param subreddit The subreddit to update
     */
    fun onFavorite(subreddit: Subreddit) {
        // Remove and add back at the correct position
        val pos = subreddits.indexOf(subreddit)
        subreddits.removeAt(pos)

        val newPos = findPosForItem(subreddit)

        subreddits.add(newPos, subreddit)

        // itemMoved just moves the item, itemChanged updates the view
        notifyItemMoved(pos, newPos)
        notifyItemChanged(newPos)
    }

    /**
     * Finds the index of where an item should be inserted into the list
     *
     * @param subreddit The subreddit to find the index for
     * @return The index the item should be inserted into
     */
    private fun findPosForItem(subreddit: Subreddit) : Int {
        var posFirstNonFavorite = 0

        for (i in 0 until subreddits.size) {
            if (!subreddits[i].isFavorited) {
                posFirstNonFavorite = i
                break
            }
        }

        // Find a sublist of where the item should go (favorite or not)
        val sublist = if (subreddit.isFavorited) {
            // Subreddit has been favorited, get the sublist of favorites
            subreddits.subList(0, posFirstNonFavorite)
        } else {
            // Sublist of everything not favorited
            subreddits.subList(posFirstNonFavorite, subreddits.size)
        }

        // binarySearch() returns the index of the item, or a negative representing where it would have been
        // The list will always be sorted on the name, so we don't have to sort it again
        var newPos = Collections.binarySearch(
                sublist,
                subreddit,
                { s1: Subreddit, s2: Subreddit -> s1.name.toLowerCase().compareTo(s2.name.toLowerCase())}
        )

        // Add one to the value and invert it to get the actual position
        // ie. newPos = -5 means it would be in position 5 (index 4)
        newPos++
        newPos *= -1

        // The pos is in the sublist, so if we're unfavoriting we need to add the favorites size
        // as that isn't counted in the sublist of non-favorites
        if (!subreddit.isFavorited) {
            newPos += posFirstNonFavorite
        }

        return newPos
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sub = subreddits[position]

        holder.binding.name.text = sub.name
        App.get().markwon.setMarkdown(holder.binding.subredditDescription, sub.publicDescription)
        ViewUtil.setSubredditIcon(holder.binding.icon, sub)

        // You can only favorite subs you are subscribed to
        holder.binding.favoriteSub.visibility = if (sub.isSubscribed) {
            holder.updateFavorited(sub.isFavorited)
            View.VISIBLE
        } else {
            View.GONE
        }

        holder.binding.nsfwTag.visibility = if (sub.isNsfw) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemSubredditBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = subreddits.size


    inner class ViewHolder(val binding: ListItemSubredditBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // The listener must be set on both the root view and the description since the description
            // has movement method and we have to check if the description is clicked on a link
            binding.root.setOnClickListener {
                val pos = adapterPosition

                if (pos != RecyclerView.NO_POSITION) {
                    subredditSelected?.subredditSelected(subreddits[pos].name)
                }
            }
            binding.subredditDescription.setOnClickListener {
                val pos = adapterPosition

                if (pos != RecyclerView.NO_POSITION &&
                        binding.subredditDescription.selectionStart == -1 &&
                        binding.subredditDescription.selectionEnd == -1
                ) {
                    subredditSelected?.subredditSelected(subreddits[pos].name)
                }
            }
            binding.favoriteSub.setOnClickListener {
                val pos = adapterPosition

                if (pos != RecyclerView.NO_POSITION) {
                    favoriteClicked?.onClick(subreddits[pos])
                }
            }
        }

        /**
         * Updates the favorite icon color
         *
         * @param favorited If true the icon will be set to show that a subreddit is favorited
         */
        fun updateFavorited(favorited: Boolean) {
            val context = binding.root.context

            binding.favoriteSub.setColorFilter(if (favorited) {
                ContextCompat.getColor(context, R.color.subredditFavorited)
            } else {
                ContextCompat.getColor(context, R.color.iconColor)
            })
        }
    }
}