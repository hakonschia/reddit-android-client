package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.hakonsreader.databinding.FragmentInboxGroupBinding

/**
 * Class for displaying an inbox group (eg. messages in the unread inbox)
 */
class InboxGroupFragment : Fragment() {
    companion object {
        /**
         * What type of inbox group this is (from [InboxFragment.InboxGroupTypes])
         */
        private const val TYPE = "type"

        fun newInstance(type: InboxFragment.InboxGroupTypes) : InboxGroupFragment {
            val args = Bundle()
            args.putInt(TYPE, type.ordinal)

            val fragment = InboxGroupFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var binding: FragmentInboxGroupBinding? = null

    var inboxType: InboxFragment.InboxGroupTypes? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inboxType = InboxFragment.InboxGroupTypes.values()[requireArguments().getInt(TYPE)]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupBinding()

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setupBinding() {
        binding = FragmentInboxGroupBinding.inflate(layoutInflater)

        when (inboxType) {
            InboxFragment.InboxGroupTypes.ALL -> binding!!.inboxGroupTitle.text = "All messages"
            InboxFragment.InboxGroupTypes.UNREAD -> binding!!.inboxGroupTitle.text = "Unread"
        }
    }

}