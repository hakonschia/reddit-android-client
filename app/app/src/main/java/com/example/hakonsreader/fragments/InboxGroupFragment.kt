package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.App
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


        /**
         * The key used to store the inbox type in saved instance states
         *
         * The value for this key should be an [Int] (the enum ordinal)
         */
        private const val SAVED_INBOX_TYPE = "saved_inboxType"

        
        fun newInstance(type: InboxFragment.InboxGroupTypes) = InboxGroupFragment().apply {
            // Should probably use arguments for this, but it doesn't get set before the tablayout
            // using the value runs, so ¯\_(ツ)_/¯
            inboxType = type
        }
    }

    private var _binding: FragmentInboxGroupBinding? = null
    private val binding get() = _binding!!
    private val db = App.get().database

    private var messageAdapter: InboxAdapter? = null
    private var messageLayoutManager: LinearLayoutManager? = null

    lateinit var inboxType: InboxFragment.InboxGroupTypes
        private set


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (savedInstanceState != null) {
            val type = savedInstanceState.getInt(SAVED_INBOX_TYPE)
            inboxType = InboxFragment.InboxGroupTypes.values()[type]
        }

        setupBinding()
        setupMessagesList()

        loadMessagesFromDb()

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVED_INBOX_TYPE, inboxType.ordinal)
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
                InboxFragment.InboxGroupTypes.ALL -> db.messages().getAllMessages()
                InboxFragment.InboxGroupTypes.UNREAD -> db.messages().getUnreadMessages()
            }

            withContext(Main) {
                messages.observe(viewLifecycleOwner, { newMessages ->
                    binding.noMessages = newMessages.isEmpty()
                    messageAdapter?.submitList(newMessages)
                })
            }
        }
    }
}