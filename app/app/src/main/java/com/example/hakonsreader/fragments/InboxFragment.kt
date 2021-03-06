package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.persistence.RedditMessagesDao
import com.example.hakonsreader.databinding.FragmentInboxBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment for displaying a users inbox
 */
@AndroidEntryPoint
class InboxFragment : Fragment() {
    companion object {
        /**
         * @return A new instance of this fragment
         */
        fun newInstance() = InboxFragment()
    }

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var messagesDao: RedditMessagesDao

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    /**
     * The fragments holding the different inbox groups
     */
    private val inboxFragments = ArrayList<InboxGroupFragment>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        _binding = FragmentInboxBinding.inflate(LayoutInflater.from(requireActivity()))

        setupTabs()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        CoroutineScope(IO).launch {
            val unread = messagesDao.getUnreadMessagesNoObservable()
            unread.let { api.messages().markRead(*it.toTypedArray()) }
            messagesDao.markAllRead()
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