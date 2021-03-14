package com.example.hakonsreader.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.misc.dpToPixels
import com.example.hakonsreader.views.ContentText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson

/**
 * Bottom sheet for peeking a text post
 */
class PeekTextPostBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val POST = "markdown"

        /**
         * Creates a new bottom sheet instance
         *
         * @param post The post to display. This should be ensured is a text post with text, no checks
         * are done in this class to verify this
         */
        fun newInstance(post: RedditPost) = PeekTextPostBottomSheet().apply {
            // Kind of weird to serialize it like this I suppose. We could just pass the text, but then
            // we can't create a ContentText.
            // By just copying over what we need (the selftext) we can at least strip away some unnecessary parsing
            val strippedPost = RedditPost().apply {
                selftext = post.selftext
            }
            arguments = bundleOf(Pair(POST, Gson().toJson(strippedPost)))
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val post = Gson().fromJson(requireArguments().getString(POST), RedditPost::class.java)

        // ContentText has a ScrollView, but if we change that to a NestedScrollView we can't scroll when
        // opening posts, and without it here we can't scroll back up without dismissing the bottom sheet
        return NestedScrollView(requireContext()).apply {
            addView(ContentText(requireContext()).apply {
                redditPost = post
            })
        }
    }
}