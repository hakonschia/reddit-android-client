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

class PeekUrlBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TEXT_KEY = "text"
        private const val URL_KEY = "url"

        fun newInstance(text: String, url: String) = PeekUrlBottomSheet().apply {
            arguments = Bundle().apply {
                putString(TEXT_KEY, text)
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
        val url = requireArguments().getString(URL_KEY)

        val openLinkListener = View.OnClickListener {
            if (url.isNullOrEmpty()) {
                return@OnClickListener
            }

            Intent(context, DispatcherActivity::class.java).apply {
                putExtra(DispatcherActivity.URL_KEY, url)
                requireContext().startActivity(this)
            }
        }

        binding.openLink.setOnClickListener(openLinkListener)
        binding.url.setOnClickListener(openLinkListener)

        binding.text.text = text
        binding.url.text = url
        binding.url.paintFlags = binding.url.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}