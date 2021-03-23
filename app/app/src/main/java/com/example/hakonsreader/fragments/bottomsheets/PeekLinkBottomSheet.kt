package com.example.hakonsreader.fragments.bottomsheets

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.databinding.BottomSheetPeekUrlBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet for peeking a link by showing the text and the URL it is pointing to
 */
class PeekLinkBottomSheet : BottomSheetDialogFragment() {

    companion object {

        /**
         * The key used in [getArguments] for the text of the link
         */
        private const val ARGS_TEXT = "args_text"

        /**
         * The key used in [getArguments] for the URL of the link
         */
        private const val ARGS_URL = "args_url"


        /**
         * Creates a new bottom sheet
         *
         * @param text The text of the URL. This will be trimmed
         * @param url The URL
         */
        fun newInstance(text: String, url: String) = PeekLinkBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARGS_TEXT, text.trim())
                putString(ARGS_URL, url)
            }
        }
    }

    private var _binding: BottomSheetPeekUrlBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPeekUrlBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val text = requireArguments().getString(ARGS_TEXT)
        val url = requireArguments().getString(ARGS_URL) ?: return

        val openLinkListener = View.OnClickListener {
            Intent(context, DispatcherActivity::class.java).apply {
                putExtra(DispatcherActivity.EXTRAS_URL_KEY, url)
                requireContext().startActivity(this)
            }
        }

        binding.text.text = text
        binding.url.text = url

        // URLs sent here might be of "/r/whatever", so assume those are links to within reddit.com
        if (!url.matches("^http(s)?.*".toRegex())) {
            binding.inferredUrlValue = "https://reddit.com" + (if (url[0] == '/') "" else "/") + url
        }

        binding.openLink.setOnClickListener(openLinkListener)
        binding.url.setOnClickListener(openLinkListener)
        binding.inferredUrl.setOnClickListener(openLinkListener)

        binding.url.paintFlags = binding.url.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.inferredUrl.paintFlags = binding.url.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}