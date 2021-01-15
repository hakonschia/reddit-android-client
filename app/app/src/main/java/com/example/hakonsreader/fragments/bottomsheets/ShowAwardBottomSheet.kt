package com.example.hakonsreader.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hakonsreader.api.model.RedditAward
import com.example.hakonsreader.databinding.BottomSheetShowAwardBinding
import com.example.hakonsreader.markwonplugins.EnlargeLinkPlugin
import com.example.hakonsreader.markwonplugins.RedditLinkPlugin
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import io.noties.markwon.Markwon

/**
 * BottomSheet for displaying information about a [RedditAward]
 */
class ShowAwardBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetShowAwardBinding? = null
    private val binding get() = _binding!!

    var award: RedditAward? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetShowAwardBinding.inflate(inflater)

        binding.award = award

        binding.description.movementMethod = InternalLinkMovementMethod.getInstance(requireContext())

        // Some descriptions have a placeholder for the web version to display an icon, but we don't
        // show those so remove the text
        val desc = award?.description?.replace("%{coin_symbol}", "") ?: ""

        // Some descriptions have subreddit links, so ensure those are linked
        // This text isn't markdown text, so using the default Markwon from App is unnecessary and inefficient
        Markwon.builder(requireContext()).usePlugin(RedditLinkPlugin()).usePlugin(EnlargeLinkPlugin()).build().apply{
            setMarkdown(binding.description, desc)
        }

        val imageUrl = award?.resizedIcons?.find { image -> image.height == 128 }?.url
        Picasso.get().load(imageUrl).into(binding.icon)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateView() {

    }
}