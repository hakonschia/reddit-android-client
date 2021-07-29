package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
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

    private var tabMediator: TabLayoutMediator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        return FragmentInboxBinding.inflate(LayoutInflater.from(requireActivity())).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTabs()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        CoroutineScope(IO).launch {
            val unread = messagesDao.getUnreadMessagesNoObservable()
            unread.let { api.messages().markRead(*it.toTypedArray()) }
            messagesDao.markAllRead()
        }

        tabMediator?.detach()
        tabMediator = null

        binding.inboxPages.adapter = null
        _binding = null
    }


    private fun setupTabs() {
        val adapter = PagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.inboxPages.adapter = adapter

        tabMediator = TabLayoutMediator(binding.inboxPagesTitle, binding.inboxPages) { tab, position ->
            tab.text = when (getInboxTypeForPosition(position)) {
                InboxGroupTypes.ALL -> getString(R.string.inboxAllMessages)
                InboxGroupTypes.UNREAD -> getString(R.string.inboxUnreadMessages)
            }
        }.also { tabLayoutMediator ->
            tabLayoutMediator.attach()
        }
    }

    private fun getInboxTypeForPosition(position: Int): InboxGroupTypes {
        return when (position) {
            0 -> InboxGroupTypes.ALL
            1 -> InboxGroupTypes.UNREAD

            else -> throw IllegalStateException("Unexpected fragment position in InboxFragment adapter: $position")
        }
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

    inner class PagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return InboxGroupFragment.newInstance(getInboxTypeForPosition(position))
        }
    }
}