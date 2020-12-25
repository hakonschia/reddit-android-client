package com.example.hakonsreader.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.BottomSheetReportsBinding
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.ReportsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/**
 * BottomSheet for displaying a list of reports
 */
class ReportsBottomSheet : BottomSheetDialogFragment() {
    companion object {
        private const val TAG = "ReportsBottomSheet"
    }

    private var _binding: BottomSheetReportsBinding? = null
    private val binding get() = _binding!!
    private val api = App.get().api

    /**
     * The post to show reports for
     */
    var post: RedditPost? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetReportsBinding.inflate(inflater)

        binding.post = post
        binding.ignored = post?.ignoreReports == true
        binding.ignoreReports.setOnClickListener { ignoreOnClick() }

        val reports = post?.userReports?.toList()
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
        post?.let {
            CoroutineScope(IO).launch {
                val ignore = !it.ignoreReports

                // Assume success
                // TODO this should notify the fragment/activity the post is in so it can update the
                //   color of the "User reports" button
                it.ignoreReports = ignore
                binding.ignored = ignore

                val resp = if (ignore) {
                    api.post(it.id).ignoreReports()
                } else {
                    api.post(it.id).unignoreReports()
                }

                when (resp) {
                    // We assume success, so nothing has to be done now
                    is ApiResponse.Success -> { }
                    is ApiResponse.Error -> {
                        // Restore original
                        it.ignoreReports = !ignore

                        // Since this is a network response, binding might have been nulled by the time
                        // the response comes through
                        _binding?.let { bind ->
                            bind.ignored = !ignore
                            Util.handleGenericResponseErrors(bind.root, resp.error, resp.throwable)
                        }
                    }
                }
            }
        }
    }

}