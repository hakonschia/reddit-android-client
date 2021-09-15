package com.example.hakonsreader.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.BottomSheetContentInfoBinding
import com.example.hakonsreader.misc.generatePostContent
import com.example.hakonsreader.views.Content
import com.example.hakonsreader.views.ContentGallery
import com.example.hakonsreader.views.ContentVideo
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson


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
         * The key used in [getArguments] for a JSON representation of the current post the content
         * is displaying
         *
         * The value with this key is a string
         */
        private const val ARGS_CURRENT_POST = "args_currentPost"


        /**
         * Creates a new bottom sheet to show information about a content view
         */
        fun newInstance(content: Content) = ContentInfoBottomSheet().apply {
            val postTitles = content.previousPosts.map { it.title }

            arguments = bundleOf(
                Pair(ARGS_CONTENT_TYPE, content.javaClass.canonicalName),
                Pair(ARGS_LIST_OF_TITLES, postTitles.toTypedArray()),
                Pair(ARGS_CURRENT_POST, Gson().toJson(content.redditPost))
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
                // Reverse the list so the newest post is first in the list
                binding.previousPostTitles.text = postTitles.reversed().joinToString("\n\n")
            }

            val currentPostJson = args.getString(ARGS_CURRENT_POST)
            if (currentPostJson != null) {
                val currentPost = Gson().fromJson(currentPostJson, RedditPost::class.java)

                val contentView = generatePostContent(requireContext(), currentPost, showTextContent = false, reusableViews = null)
                if (contentView != null) {
                    if (contentView is ContentVideo) {
                        contentView.observeVideoLifecycle(viewLifecycleOwner)
                    } else if (contentView is ContentGallery) {
                        contentView.lifecycleOwner = viewLifecycleOwner
                    }

                    contentView.redditPost = currentPost
                    binding.currentPostContent.addView(contentView)
                }

                binding.currentPost = currentPost
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}