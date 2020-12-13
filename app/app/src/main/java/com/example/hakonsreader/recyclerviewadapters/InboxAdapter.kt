package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.databinding.ListItemInboxMessageBinding
import com.example.hakonsreader.recyclerviewadapters.diffutils.MessagesDiffCallback

class InboxAdapter : RecyclerView.Adapter<InboxAdapter.ViewHolder>()  {
    private var messages = ArrayList<RedditMessage>()

    fun submitList(newMessages: List<RedditMessage>) {
        val old = messages
        messages = newMessages as ArrayList<RedditMessage>

        DiffUtil.calculateDiff(
                MessagesDiffCallback(old, messages)
        ).dispatchUpdatesTo(this)
    }

    fun getMessages() : List<RedditMessage> = messages

    override fun onBindViewHolder(holder: InboxAdapter.ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxAdapter.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(ListItemInboxMessageBinding.inflate(layoutInflater, parent, false))
    }

    override fun getItemCount(): Int = messages.size


    inner class ViewHolder(private val binding: ListItemInboxMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: RedditMessage) {
            binding.message = message
            binding.executePendingBindings()
        }
    }
}