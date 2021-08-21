package com.example.hakonsreader.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.BottomSheetContentInfoBinding
import com.example.hakonsreader.views.Content
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


/**
 * Bottom sheet for displaying debug info about a [Content] view
 */
class ContentInfoBottomSheet : BottomSheetDialogFragment() {

    companion object {

        /**
         * The key used in [getArguments] for the name of the content class that has been recycled
         *
         * The value with this key is a string
         */
        private const val ARGS_CONTENT_TYPE = "args_contentType"

        /**
         * The key used in [getArguments] for the list of the previous post titles
         *
         * The value with this key is a string array
         */
        private const val ARGS_LIST_OF_TITLES = "args_listOfTitles"


        /**
         * Creates a new bottom sheet to show information about a content view
         *
         * @param contentType The content class that is being shown
         * @param posts The posts describing the content info
         */
        fun newInstance(contentType: Class<out Content>, posts: List<RedditPost>) = ContentInfoBottomSheet().apply {
            val postTitles = posts.map { it.title }

            arguments = bundleOf(
                Pair(ARGS_CONTENT_TYPE, contentType.canonicalName),
                Pair(ARGS_LIST_OF_TITLES, postTitles.toTypedArray())
            )
        }
    }

    private var _binding: BottomSheetContentInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return BottomSheetContentInfoBinding.inflate(inflater).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireArguments().let { args ->
            binding.className.text = args.getString(ARGS_CONTENT_TYPE, "")

            args.getStringArray(ARGS_LIST_OF_TITLES)?.let { postTitles ->
                binding.numPreviousPosts = postTitles.size

                // Two newlines for some padding between
                binding.previousPostTitles.text = postTitles.joinToString("\n\n")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}