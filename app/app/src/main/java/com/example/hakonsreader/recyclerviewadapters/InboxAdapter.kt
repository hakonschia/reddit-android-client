package com.example.hakonsreader.recyclerviewadapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.BR
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.databinding.InboxCommentBinding
import com.example.hakonsreader.databinding.InboxMessageBinding
import com.example.hakonsreader.misc.createAgeText
import com.example.hakonsreader.recyclerviewadapters.diffutils.MessagesDiffCallback
import com.example.hakonsreader.views.ListDivider
import java.time.Duration
import java.time.Instant

class InboxAdapter : RecyclerView.Adapter<InboxAdapter.ViewHolder>()  {
    companion object {
        /**
         * The type returned from [getItemViewType] when the item is a message from a comment reply
         */
        private const val TYPE_COMMENT = 0

        /**
         * The type returned from [getItemViewType] when the item is a message from a private message
         */
        private const val TYPE_MESSAGE = 1
    }


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

        return when (viewType) {
            TYPE_COMMENT -> ViewHolder(InboxCommentBinding.inflate(layoutInflater, parent, false))
            else -> ViewHolder(InboxMessageBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].wasComment) TYPE_COMMENT else TYPE_MESSAGE
    }

    override fun getItemCount(): Int = messages.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val divider = ListDivider(ContextCompat.getDrawable(recyclerView.context, R.drawable.list_divider_no_padding))
        recyclerView.addItemDecoration(divider)
    }

    inner class ViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: RedditMessage) {
            binding.setVariable(BR.message, message)
            binding.executePendingBindings()
        }
    }
}

@BindingAdapter("inboxSentAgo")
fun setSentAgoText(textView: TextView, createdAt: Long) {
    val created = Instant.ofEpochSecond(createdAt)
    val now = Instant.now()
    val between = Duration.between(created, now)
    val createdAtText = createAgeText(textView.resources, between)

    textView.text = textView.resources.getString(R.string.inboxSent, createdAtText)
}

/**
 * Opens a message context, ie. the post the message is from
 *
 * @param view The view clicked
 * @param context The context for a message ([RedditMessage.context])
 */
fun openMessageContext(view: View, context: String) {
    val finalContext = "https://reddit.com$context"

    val intent = Intent(view.context, DispatcherActivity::class.java)
    intent.putExtra(DispatcherActivity.EXTRAS_URL_KEY, finalContext)

    view.context.startActivity(intent)
}