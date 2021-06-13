package com.example.hakonsreader.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.hakonsreader.R
import com.example.hakonsreader.databinding.BottomSheetVideoPlaybackErrorBinding
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


/**
 * BottomSheet for displaying error information about why a video failed to load
 */
class VideoPlaybackErrorBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARGS_URL = "args_url"

        private const val ARGS_EXCEPTION = "agrs_exception"

        fun newInstance(error: ExoPlaybackException, url: String) = VideoPlaybackErrorBottomSheet().apply {
            arguments = bundleOf(
                ARGS_URL to url,
                ARGS_EXCEPTION to error.stackTraceToString()
            )
        }
    }

    private var _binding: BottomSheetVideoPlaybackErrorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return BottomSheetVideoPlaybackErrorBinding.inflate(inflater).apply {
            _binding = this
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        val url = args.getString(ARGS_URL) ?: ""
        val exception = args.getString(ARGS_EXCEPTION) ?: ""

        binding.videoUrl.text = if (url.isNotEmpty()) {
            url
        } else {
            getString(R.string.videoPlaybackErrorBottomSheetUrlEmpty)
        }

        binding.exception.text = exception
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}