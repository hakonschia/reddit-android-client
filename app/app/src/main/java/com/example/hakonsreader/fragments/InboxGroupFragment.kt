package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.databinding.FragmentInboxGroupBinding

/**
 * Class for displaying an inbox group (eg. messages in the unread inbox)
 */
class InboxGroupFragment : Fragment() {
    companion object {
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

    lateinit var inboxType: InboxFragment.InboxGroupTypes
        private set


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupBinding()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupBinding() {
        _binding = FragmentInboxGroupBinding.inflate(layoutInflater)

        when (inboxType) {
            InboxFragment.InboxGroupTypes.ALL -> binding.inboxGroupTitle.text = getString(R.string.inboxAllMessages)
            InboxFragment.InboxGroupTypes.UNREAD -> binding.inboxGroupTitle.text = getString(R.string.inboxUnreadMessages)
        }
    }

}