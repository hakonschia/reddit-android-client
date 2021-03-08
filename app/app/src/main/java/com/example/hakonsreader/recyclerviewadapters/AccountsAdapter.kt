package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditUserInfo
import com.example.hakonsreader.databinding.ListItemAccountBinding

class AccountsAdapter : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {


    /**
     * Callback for when an item in the list has been clicked
     */
    var onItemClicked: ((RedditUserInfo) -> Unit)? = null

    /**
     * Callback for when "Remove account" has been clicked
     */
    var onRemoveItemClicked: ((RedditUserInfo) -> Unit)? = null

    /**
     * Callback for when "NSFW checkbox" has been clicked. The account clicked and the new value
     * is passed back
     */
    var onNsfwClicked: ((RedditUserInfo, Boolean) -> Unit)? = null


    var accounts: MutableList<RedditUserInfo> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun removeItem(account: RedditUserInfo) {
        val pos = accounts.indexOf(account)
        if (pos != -1) {
            accounts.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = accounts[position]

        with (holder.binding) {
            this.username = account.userInfo?.username

            // Highlight the currently active account
            this.highlight = App.get().getUserInfo()?.userId == account.userId

            this.nsfwAccount = account.nsfwAccount
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
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClicked?.invoke(accounts[adapterPosition])
                }
            }
            binding.removeAccount.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onRemoveItemClicked?.invoke(accounts[adapterPosition])
                }
            }
            binding.nsfwCheckBox.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onNsfwClicked?.invoke(accounts[adapterPosition], binding.nsfwCheckBox.isChecked)
                }
            }
        }
    }
}
