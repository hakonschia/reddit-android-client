package com.example.hakonsreader.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.FragmentInboxBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/**
 * Fragment for displaying a users inbox
 */
class InboxFragment : Fragment() {

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    /**
     * The fragments holding the different inbox groups
     */
    private val inboxFragments = ArrayList<InboxGroupFragment>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        _binding = FragmentInboxBinding.inflate(layoutInflater)

        setupTabs()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Send API request to mark messages as unread when the view is destroyed
        val db = RedditDatabase.getInstance(requireContext())
        val api = App.get().api

        CoroutineScope(IO).launch {
            val unread = db.messages().unreadMessages
            // Not sure if it makes sense to only mark the messages as read in the inbox if it's a success
            unread?.value?.toTypedArray()?.let { api.messages().markRead(*it) }
            db.messages().markRead()
        }

        _binding = null
    }


    private fun setupTabs() {
        // If the fragment is shown multiple times, don't add the tabs multiple times as well
        if (inboxFragments.isEmpty()) {
            inboxFragments.add(InboxGroupFragment.newInstance(InboxGroupTypes.ALL))
            inboxFragments.add(InboxGroupFragment.newInstance(InboxGroupTypes.UNREAD))
        }

        binding.inboxPages.adapter = PagerAdapter(requireActivity())
        TabLayoutMediator(binding.inboxPagesTitle, binding.inboxPages) { tab, position ->
            tab.text = when (inboxFragments[position].inboxType) {
                InboxGroupTypes.ALL -> getString(R.string.inboxAllMessages)
                InboxGroupTypes.UNREAD -> getString(R.string.inboxUnreadMessages)
            }
        }.attach()
    }


    enum class InboxGroupTypes {
        /**
         * All messages in the inbox (read & unread)
         */
        ALL,

        /**
         * Unread messages in the inbox
         */
        UNREAD

        // TODO message (private message sent to me)
        //  sent messages (private messages sent from me)
    }

    inner class PagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = inboxFragments.size
        override fun createFragment(position: Int): Fragment = inboxFragments[position]
    }
}