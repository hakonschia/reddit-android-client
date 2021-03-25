package com.example.hakonsreader.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.interfaces.ReportableListing
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.BottomSheetReportsBinding
import com.example.hakonsreader.interfaces.OnReportsIgnoreChangeListener
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.example.hakonsreader.recyclerviewadapters.ReportsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * BottomSheet for displaying a list of reports
 */
@AndroidEntryPoint
class ReportsBottomSheet : BottomSheetDialogFragment() {
    companion object {
        private const val TAG = "ReportsBottomSheet"

        /**
         * The key used in [getArguments] for the listing to display in the bottom sheet
         *
         * The value with this key should be a JSON string representing a [ReportableListing]
         */
        private const val ARGS_LISTING = "args_listing"


        /**
         * Creates a new bottom sheet to show reports on listing
         *
         * @param listing The reportable listing to show
         */
        fun newInstance(listing: ReportableListing) = ReportsBottomSheet().apply {
            arguments = bundleOf(Pair(ARGS_LISTING, Gson().toJson(listing)))
        }
    }

    @Inject
    lateinit var api: RedditApi

    private var _binding: BottomSheetReportsBinding? = null
    private val binding get() = _binding!!


    private lateinit var listing: ReportableListing

    /**
     * The listener to call when the reports have been set to be ignored/unignored
     */
    var onIgnoreChange: OnReportsIgnoreChangeListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val listingAsJson = requireArguments().getString(ARGS_LISTING) ?: throw IllegalStateException("No listing given")
        val asJsonObject = JsonParser.parseString(listingAsJson).asJsonObject
        val kind = asJsonObject.get("kind").asString

        listing = when (kind) {
            Thing.POST.value -> Gson().fromJson(listingAsJson, RedditPost::class.java)
            Thing.COMMENT.value -> Gson().fromJson(listingAsJson, RedditComment::class.java)
            else -> throw IllegalStateException("Unknown listing type: $kind")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetReportsBinding.inflate(inflater)

        binding.ignored = listing.ignoreReports == true
        binding.ignoreReports.setOnClickListener { ignoreOnClick() }
        binding.dismissedReports = listing.userReports?.isEmpty() == true

        val reports = if (listing.userReports?.isNotEmpty() == true) {
            listing.userReports
        } else {
            listing.userReportsDismissed
        }?.toList()

        val adapter = ReportsAdapter().apply {
            if (reports != null) {
                submitList(reports)
            }
        }

        binding.reports.layoutManager = LinearLayoutManager(context)
        binding.reports.adapter = adapter

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun ignoreOnClick() {
        CoroutineScope(IO).launch {
            val ignore = !listing.ignoreReports

            // Assume success
            listing.ignoreReports = ignore
            binding.ignored = ignore
            withContext(Main) {
                onIgnoreChange?.onIgnoredChange(ignore)
            }

            val request = if (listing is RedditPost) {
                api.post(listing.id)
            } else {
                api.comment(listing.id)
            }

            val resp = if (ignore) {
                request.ignoreReports()
            } else {
                request.unignoreReports()
            }

            when (resp) {
                // We assume success, so nothing has to be done now
                is ApiResponse.Success -> { }
                is ApiResponse.Error -> {
                    withContext(Main) {
                        // Restore original
                        listing.ignoreReports = !ignore
                        onIgnoreChange?.onIgnoredChange(!ignore)

                        // Since this is a network response, binding might have been nulled by the time
                        // the response comes through
                        _binding?.let { bind ->
                            bind.ignored = !ignore
                            handleGenericResponseErrors(bind.root, resp.error, resp.throwable)
                        }
                    }
                }
            }
        }
    }
}