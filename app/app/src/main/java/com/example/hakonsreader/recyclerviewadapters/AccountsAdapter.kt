package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditUserInfo
import com.example.hakonsreader.databinding.ListItemAccountBinding

class AccountsAdapter : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {
    var accounts: List<RedditUserInfo> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = accounts[position]

        with (holder.binding) {
            this.username = account.userInfo?.username

            // Highlight the currently active account
            this.highlight = App.get().currentUserInfo?.userId == account.userId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemAccountBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount() = accounts.size

    inner class ViewHolder(val binding: ListItemAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            val app = App.get()
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    app.switchAccount(accounts[adapterPosition].accessToken)
                }
            }
        }
    }
}
