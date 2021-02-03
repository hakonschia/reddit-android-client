package com.example.hakonsreader.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.databinding.BottomSheetPeekParentCommentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * BottomSheet for peeking a comment
 */
class PeekCommentBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPeekParentCommentBinding? = null
    private val binding get() = _binding!!

    /**
     * The comment to show
     */
    var comment: RedditComment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetPeekParentCommentBinding.inflate(inflater)
        binding.isByLoggedInUser = comment?.author == App.get().currentUserInfo?.userInfo?.username
        binding.comment = comment

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}