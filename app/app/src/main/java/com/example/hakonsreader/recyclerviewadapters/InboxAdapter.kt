package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.databinding.ListItemInboxMessageBinding

class InboxAdapter : RecyclerView.Adapter<InboxAdapter.ViewHolder>()  {
    private var messages = ArrayList<RedditMessage>()


    fun submitList(newMessages: List<RedditMessage>) {
        messages = newMessages as ArrayList<RedditMessage>
        notifyDataSetChanged()
    }

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