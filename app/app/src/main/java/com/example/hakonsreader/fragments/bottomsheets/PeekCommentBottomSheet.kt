package com.example.hakonsreader.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.databinding.BottomSheetPeekParentCommentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson

/**
 * BottomSheet for peeking a comment
 */
class PeekCommentBottomSheet : BottomSheetDialogFragment() {

    companion object {

        /**
         * The key used in [getArguments] for the comment to display in the bottom sheet
         *
         * The value with this key should be a JSON string representing the comment
         */
        private const val ARGS_COMMENTS = "args_comment"


        /**
         * Creates a new bottom sheet to peek a comment
         *
         * @param comment The comment to peek
         */
        fun newInstance(comment: RedditComment) = PeekCommentBottomSheet().apply {
            arguments = bundleOf(Pair(ARGS_COMMENTS, Gson().toJson(comment)))
        }
    }

    private var _binding: BottomSheetPeekParentCommentBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetPeekParentCommentBinding.inflate(inflater)

        val comment = Gson().fromJson(requireArguments().getString(ARGS_COMMENTS), RedditComment::class.java)

        binding.isByLoggedInUser = comment?.author == App.get().getUserInfo()?.userInfo?.username
        binding.comment = comment

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}