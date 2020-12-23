package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.BottomSheetReportsBinding
import com.example.hakonsreader.recyclerviewadapters.ReportsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * BottomSheet for displaying a list of reports
 */
class ReportsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetReportsBinding? = null
    private val binding get() = _binding!!

    /**
     * The post to show reports for
     */
    var post: RedditPost? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetReportsBinding.inflate(inflater)

        binding.title.text = getString(R.string.userReportsBottomSheetTitle, post?.title)

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

}