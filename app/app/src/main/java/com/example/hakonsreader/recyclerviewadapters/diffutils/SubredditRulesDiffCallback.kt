package com.example.hakonsreader.recyclerviewadapters.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.example.hakonsreader.api.model.SubredditRule

/**
 * Diff callback for lists of [SubredditRule]
 */
class SubredditRulesDiffCallback(
        private val oldList: List<SubredditRule>,
        private val newList: List<SubredditRule>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]

        return old.name == new.name
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]

        return old.description == new.description
                && old.priority == new.priority
    }
}