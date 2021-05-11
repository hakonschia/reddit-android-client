package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.api.model.RedditMulti
import com.example.hakonsreader.databinding.ListItemRedditMultiBinding
import com.example.hakonsreader.misc.dpToPixels
import com.example.hakonsreader.recyclerviewadapters.diffutils.RedditMultiDiffCallback
import com.example.hakonsreader.views.SpaceDivider
import com.squareup.picasso.Picasso

class RedditMultiAdapter : RecyclerView.Adapter<RedditMultiAdapter.ViewHolder>() {

    private var multis: List<RedditMulti> = ArrayList()

    var onMultiSelected: ((RedditMulti) -> Unit)? = null

    fun submitList(list: List<RedditMulti>) {
        val previous = multis


        val diffResults = DiffUtil.calculateDiff(
                RedditMultiDiffCallback(previous, list),
                true
        )

        multis = list
        diffResults.dispatchUpdatesTo(this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val multi = multis[position]

        holder.binding.multi = multi

        Picasso.get()
                .load(multi.iconUrl)
                .into(holder.binding.icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                ListItemRedditMultiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.addItemDecoration(SpaceDivider(dpToPixels(8f, recyclerView.resources)))
    }

    override fun getItemCount() = multis.size

    inner class ViewHolder(val binding: ListItemRedditMultiBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMultiSelected?.invoke(multis[pos])
                }
            }
        }
    }
}