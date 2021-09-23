package com.example.hakonsreader.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.example.hakonsreader.api.model.RedditAward
import com.example.hakonsreader.databinding.BottomSheetShowAwardBinding
import com.example.hakonsreader.markwonplugins.EnlargeLinkPlugin
import com.example.hakonsreader.markwonplugins.RedditLinkPlugin
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.misc.Settings
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import javax.inject.Inject

/**
 * BottomSheet for displaying information about a [RedditAward]
 */
@AndroidEntryPoint
class ShowAwardBottomSheet : BottomSheetDialogFragment() {
    companion object {
        /**
         * The size of the images of the awards the bottom sheet will load
         */
        const val IMAGE_SIZE = 128


        /**
         * The key used in [getArguments] to get the award to display
         *
         * The value with this key should be a JSON string representing the award
         */
        private const val ARGS_AWARD = "args_award"

        fun newInstance(award: RedditAward) = ShowAwardBottomSheet().apply {
            arguments = bundleOf(Pair(ARGS_AWARD, Gson().toJson(award)))
        }
    }

    private var _binding: BottomSheetShowAwardBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var settings: Settings


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetShowAwardBinding.inflate(LayoutInflater.from(requireActivity()))

        val award = Gson().fromJson(requireArguments().getString(ARGS_AWARD), RedditAward::class.java)

        binding.award = award
        binding.description.movementMethod = InternalLinkMovementMethod()

        // Some descriptions have a placeholder for the web version to display an icon, but we don't
        // show those so remove the text
        val desc = award?.description?.replace("%{coin_symbol}", "") ?: ""

        // Some descriptions have subreddit links, so ensure those are linked
        // This text isn't markdown text, so using the default Markwon from App is unnecessary and inefficient
        Markwon.builder(requireContext()).usePlugin(RedditLinkPlugin()).usePlugin(EnlargeLinkPlugin(settings)).build().apply {
            setMarkdown(binding.description, desc)
        }

        val imageUrl = award?.resizedIcons?.find { image -> image.height == IMAGE_SIZE }?.url
        Glide.with(this)
            .load(imageUrl)
            .override(IMAGE_SIZE, IMAGE_SIZE)
            .into(binding.icon)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}