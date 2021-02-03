package com.example.hakonsreader.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditUserInfo
import com.example.hakonsreader.databinding.ListItemAccountBinding
import com.example.hakonsreader.interfaces.OnClickListener

class AccountsAdapter : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {
    /**
     * Click listener for items in the list
     */
    var onItemClicked: OnClickListener<RedditUserInfo>? = null

    /**
     * Click listener for when "Remove account" has been clicked
     */
    var onRemoveItem: OnClickListener<RedditUserInfo>? = null

    /**
     * Click listener for when "NSFW checkbox" has been clicked. When this is clicked the value at
     * [RedditUserInfo.nsfwAccount] will be updated based on the checkbox value and the user info object
     * is passed to the callback
     */
    var onNsfwClicked: OnClickListener<RedditUserInfo>? = null


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
            this.highlight = App.get().currentUserInfo?.userId == account.userId

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
                    onItemClicked?.onClick(accounts[adapterPosition])
                }
            }
            binding.removeAccount.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onRemoveItem?.onClick(accounts[adapterPosition])
                }
            }
            binding.nsfwCheckBox.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    accounts[adapterPosition].run {
                        nsfwAccount = binding.nsfwCheckBox.isChecked
                        onNsfwClicked?.onClick(this)
                    }
                }
            }
        }
    }
}
