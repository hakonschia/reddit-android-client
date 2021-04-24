package com.example.hakonsreader.recyclerviewadapters.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.example.hakonsreader.api.model.RedditMulti

/**
 * Callback class for DiffUtil for lists of [RedditMulti]
 */
class RedditMultiDiffCallback(private val oldList: List<RedditMulti>, private val newList: List<RedditMulti>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        // Multis don't have a name, but the same owner cannot have two multis with the same name
        return oldItem.owner == newItem.owner
                && oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return oldItem.name == newItem.name
                && oldItem.isFavorited == newItem.isFavorited
                && oldItem.isSubscribed == newItem.isSubscribed
    }
}