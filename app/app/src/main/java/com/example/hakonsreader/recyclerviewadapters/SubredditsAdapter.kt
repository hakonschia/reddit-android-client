package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.databinding.ListItemSubredditBinding
import com.example.hakonsreader.databinding.ListItemSubredditSimpleBinding
import com.example.hakonsreader.interfaces.OnClickListener
import com.example.hakonsreader.interfaces.OnSubredditSelected
import com.example.hakonsreader.recyclerviewadapters.diffutils.SubredditsDiffCallback
import java.util.*
import kotlin.collections.ArrayList

/**
 * Adapter for displaying a list of [Subreddit] items
 */
class SubredditsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class SubredditViewType {
        SIMPLE,
        STANDARD
    }


    private var subreddits: List<Subreddit> = ArrayList()

    /**
     * The listener for when a subreddit in the list has been clicked
     */
    var subredditSelected: OnSubredditSelected? = null

    /**
     * The listener for when the "Favorite" icon in a list item has been clicked
     */
    var favoriteClicked: OnClickListener<Subreddit>? = null

    var viewType = SubredditViewType.STANDARD


    /**
     * Submit the list of subreddits to display
     *
     * @param list The list of items to display
     * @param sort If set to true the list will be sorted in the following order:
     * - Favorites (for logged in users)
     * - The rest of the subreddits
     * - Users the user is following
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
        subreddits = ArrayList()
        notifyItemRangeRemoved(0, size)
    }

    /**
     * Sorts a list of subreddits
     *
     * The order of the list will be, sorted alphabetically:
     * * Favorites (for logged in users)
     * * All subreddits, including duplicates of favorites, of the subreddits
     * * Users the user is following
     *
     * @param list The list so sort
     * @return A new list that is sorted based on the subreddit type
     */
    private fun sortSubreddits(list: List<Subreddit>) : List<Subreddit> {
        val sorted = list.sortedBy { it.name.toLowerCase(Locale.ROOT) }.toMutableList()
        val favorites = sorted.filter { s -> s.isFavorited }
        val users = sorted.filter { s -> s.subredditType == "user"}

        // Remove users from this list so they are only at the bottom
        sorted.removeAll(users)

        // Return all combined in the correct order
        return ArrayList<Subreddit>().apply {
            addAll(favorites)
            addAll(sorted)
            addAll(users)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val sub = subreddits[position]

        when (viewType) {
            SubredditViewType.SIMPLE -> {
                (holder as SimpleViewHolder).bind(sub)
            }
            SubredditViewType.STANDARD -> {
                (holder as StandardViewHolder).bind(sub)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SubredditViewType.SIMPLE -> {
                val binding = ListItemSubredditSimpleBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )

                SimpleViewHolder(binding)
            }
            SubredditViewType.STANDARD -> {
                val binding = ListItemSubredditBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )

                StandardViewHolder(binding)
            }
        }
    }

    override fun getItemCount() = subreddits.size

    inner class SimpleViewHolder(val binding: ListItemSubredditSimpleBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            with(binding) {
                root.setOnClickListener {
                    val pos = adapterPosition

                    if (pos != RecyclerView.NO_POSITION) {
                        subredditSelected?.subredditSelected(subreddits[pos].name)
                    }
                }

                favoriteSub.setOnClickListener {
                    val pos = adapterPosition

                    if (pos != RecyclerView.NO_POSITION) {
                        favoriteClicked?.onClick(subreddits[pos])
                    }
                }
            }
        }

        fun bind(subreddit: Subreddit) {
            binding.subreddit = subreddit
            binding.executePendingBindings()
        }
    }

    inner class StandardViewHolder(val binding: ListItemSubredditBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            with(binding) {
                // The listener must be set on both the root view and the description since the description
                // has movement method and we have to check if the description is clicked on a link
                root.setOnClickListener {
                    val pos = adapterPosition

                    if (pos != RecyclerView.NO_POSITION) {
                        subredditSelected?.subredditSelected(subreddits[pos].name)
                    }
                }

                subredditDescription.setOnClickListener {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION &&
                            subredditDescription.selectionStart == -1 &&
                            subredditDescription.selectionEnd == -1
                    ) {
                        subredditSelected?.subredditSelected(subreddits[pos].name)
                    }
                }

                favoriteSub.setOnClickListener {
                    val pos = adapterPosition

                    if (pos != RecyclerView.NO_POSITION) {
                        favoriteClicked?.onClick(subreddits[pos])
                    }
                }
            }
        }

        fun bind(subreddit: Subreddit) {
            binding.subreddit = subreddit
            binding.executePendingBindings()
        }
    }
}