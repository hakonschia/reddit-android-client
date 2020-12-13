package com.example.hakonsreader.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.FragmentInboxGroupBinding
import com.example.hakonsreader.recyclerviewadapters.InboxAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Class for displaying an inbox group (eg. messages in the unread inbox)
 */
class InboxGroupFragment : Fragment() {
    companion object {
        private const val TAG = "InboxGroupFragment"
        
        fun newInstance(type: InboxFragment.InboxGroupTypes) : InboxGroupFragment {
            // Should probably use arguments for this, but it doesn't get set before the tablayout
            // using the value runs, so ¯\_(ツ)_/¯
            val fragment = InboxGroupFragment()
            fragment.inboxType = type
            return fragment
        }
    }

    private var _binding: FragmentInboxGroupBinding? = null
    private val binding get() = _binding!!
    private val api = App.get().api
    private val db by lazy {
        RedditDatabase.getInstance(requireContext())
    }

    private var messageAdapter: InboxAdapter? = null
    private var messageLayoutManager: LinearLayoutManager? = null

    lateinit var inboxType: InboxFragment.InboxGroupTypes
        private set


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupBinding()
        setupMessagesList()

        loadMessagesFromDb()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupBinding() {
        _binding = FragmentInboxGroupBinding.inflate(layoutInflater)
    }

    private fun setupMessagesList() {
        messageLayoutManager = LinearLayoutManager(requireContext())
        messageAdapter = InboxAdapter()

        binding.messages.layoutManager = messageLayoutManager
        binding.messages.adapter = messageAdapter
    }

    /**
     * Loads messages from the database, based on [inboxType]
     *
     * If [inboxType] is [InboxFragment.InboxGroupTypes.UNREAD], then an API request are sent to
     * mark the messages as read
     */
    private fun loadMessagesFromDb() {
        CoroutineScope(IO).launch {
            val messages = when (inboxType) {
                InboxFragment.InboxGroupTypes.ALL -> db.messages().allMessages
                InboxFragment.InboxGroupTypes.UNREAD -> {
                    val unread = db.messages().unreadMessages

                    when (api.messages().markRead(*unread.toTypedArray())) {
                        is ApiResponse.Success -> {
                            Log.d(TAG, "loadMessagesFromDb: marked as unread")
                            db.messages().markRead()
                        }
                        is ApiResponse.Error -> {}
                    }

                    unread
                }
            }

            withContext(Main) {
                messageAdapter?.submitList(messages)
            }
        }
    }

}