package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.SubredditRule
import com.example.hakonsreader.databinding.ListItemSubredditRuleBinding
import com.example.hakonsreader.recyclerviewadapters.diffutils.SubredditRulesDiffCallback
import com.example.hakonsreader.views.ListDivider
import com.example.hakonsreader.views.util.setLongClickToPeekUrl

/**
 * RecyclerView adapter for displaying [SubredditRule]
 */
class SubredditRulesAdapter : RecyclerView.Adapter<SubredditRulesAdapter.ViewHolder>() {

    private var rules: List<SubredditRule> = ArrayList()

    fun submitList(list: List<SubredditRule>) {
        val previous = rules

        val diffResults = DiffUtil.calculateDiff(
                SubredditRulesDiffCallback(previous, list),
                true
        )

        rules = list
        diffResults.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ListItemSubredditRuleBinding.inflate(
                layoutInflater,
                parent,
                false
        ).apply {
            description.setLongClickToPeekUrl()
        }

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rules[position])
    }

    override fun getItemCount() = rules.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val divider = ListDivider(ContextCompat.getDrawable(recyclerView.context, R.drawable.list_divider))
        recyclerView.addItemDecoration(divider)
    }

    inner class ViewHolder(val binding: ListItemSubredditRuleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rule: SubredditRule) {
            with(binding) {
                this.rule = rule
                number = adapterPosition + 1
                executePendingBindings()
            }
        }
    }

}