package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.databinding.PeekParentCommentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PeekCommentBottomSheetDialog : BottomSheetDialogFragment() {

    private var binding: PeekParentCommentBinding? = null

    /**
     * The comment to show
     */
    var comment: RedditComment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PeekParentCommentBinding.inflate(inflater)
        binding?.isByLoggedInUser = comment?.author == App.getStoredUser()?.username
        binding?.comment = comment

        return binding?.root
    }

}