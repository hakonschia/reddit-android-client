package com.example.hakonsreader.recyclerviewadapters.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.example.hakonsreader.api.model.RedditMessage

class MessagesDiffCallback(
        private val oldList: List<RedditMessage>,
        private val newList: List<RedditMessage>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        // The only thing that will change in the message items are the read status
        // (I think)
        return oldItem.isNew == newItem.isNew
    }
}