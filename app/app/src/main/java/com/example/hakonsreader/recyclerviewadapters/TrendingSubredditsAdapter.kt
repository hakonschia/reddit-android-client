package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.databinding.ListItemTrendingSubredditBinding

class TrendingSubredditsAdapter : RecyclerView.Adapter<TrendingSubredditsAdapter.ViewHolder>() {

    private var trendingSubreddits: List<String> = ArrayList()
    var onSubredditSelected: ((String) -> Unit)? = null

    fun submitList(trendingSubreddits: List<String>) {
        this.trendingSubreddits = trendingSubreddits
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.name.text = trendingSubreddits[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemTrendingSubredditBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount() = trendingSubreddits.size

    inner class ViewHolder(val binding: ListItemTrendingSubredditBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onSubredditSelected?.invoke(trendingSubreddits[adapterPosition])
                }
            }
        }
    }
}
