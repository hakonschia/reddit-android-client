package com.example.hakonsreader.fragments.bottomsheets

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.databinding.BottomSheetPeekUrlBinding
import com.example.hakonsreader.misc.CreateIntentOptions
import com.example.hakonsreader.misc.createIntent
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PeekUrlBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TEXT_KEY = "text"
        private const val URL_KEY = "url"

        /**
         * Creates a new bottom sheet
         *
         * @param text The text of the URL. This will be trimmed
         * @param url The URL
         */
        fun newInstance(text: String, url: String) = PeekUrlBottomSheet().apply {
            arguments = Bundle().apply {
                putString(TEXT_KEY, text.trim())
                putString(URL_KEY, url)
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
        val text = requireArguments().getString(TEXT_KEY)
        val url = requireArguments().getString(URL_KEY) ?: return

        val openLinkListener = View.OnClickListener {
            Intent(context, DispatcherActivity::class.java).apply {
                putExtra(DispatcherActivity.URL_KEY, url)
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