package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.persistence.RedditMessagesDao
import com.example.hakonsreader.databinding.FragmentInboxGroupBinding
import com.example.hakonsreader.recyclerviewadapters.InboxAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Class for displaying an inbox group (eg. messages in the unread inbox)
 */
@AndroidEntryPoint
class InboxGroupFragment : Fragment() {

    companion object {
        @Suppress("UNUSED")
        private const val TAG = "InboxGroupFragment"


        /**
         * The key used to store the inbox type in saved instance states
         *
         * The value for this key should be an [Int] (the enum ordinal)
         */
        private const val SAVED_INBOX_TYPE = "saved_inboxType"

        
        fun newInstance(type: InboxFragment.InboxGroupTypes) = InboxGroupFragment().apply {
            // Should probably use arguments for this, but it doesn't get set before the tab layout
            // using the value runs, so ¯\_(ツ)_/¯
            inboxType = type
        }
    }

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var messagesDao: RedditMessagesDao

    private var _binding: FragmentInboxGroupBinding? = null
    private val binding get() = _binding!!

    lateinit var inboxType: InboxFragment.InboxGroupTypes
        private set


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentInboxGroupBinding.inflate(LayoutInflater.from(requireActivity())).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val type = savedInstanceState.getInt(SAVED_INBOX_TYPE)
            inboxType = InboxFragment.InboxGroupTypes.values()[type]
        }

        setupMessagesList()

        loadMessagesFromDb()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVED_INBOX_TYPE, inboxType.ordinal)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupMessagesList() {
        binding.messages.layoutManager = LinearLayoutManager(requireContext())
        binding.messages.adapter = InboxAdapter(api, messagesDao)
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
                InboxFragment.InboxGroupTypes.ALL -> messagesDao.getAllMessages()
                InboxFragment.InboxGroupTypes.UNREAD -> messagesDao.getUnreadMessages()
            }

            withContext(Main) {
                messages.observe(viewLifecycleOwner, { newMessages ->
                    binding.noMessages = newMessages.isEmpty()

                    (binding.messages.adapter as InboxAdapter).submitList(newMessages)
                })
            }
        }
    }
}