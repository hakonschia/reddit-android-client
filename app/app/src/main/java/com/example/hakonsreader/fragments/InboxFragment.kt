package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.hakonsreader.databinding.FragmentInboxBinding

/**
 * Fragment for displaying a users inbox
 */
class InboxFragment : Fragment() {

    private var binding: FragmentInboxBinding? = null

    /**
     * The fragments holding the different inbox groups
     */
    private val inboxFragments = ArrayList<InboxGroupFragment>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentInboxBinding.inflate(layoutInflater)

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


    private fun setupTabs() {
        inboxFragments.add(InboxGroupFragment.newInstance(InboxGroupTypes.ALL))
        inboxFragments.add(InboxGroupFragment.newInstance(InboxGroupTypes.UNREAD))
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