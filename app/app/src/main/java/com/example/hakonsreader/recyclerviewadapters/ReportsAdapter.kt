package com.example.hakonsreader.recyclerviewadapters

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.R

class ReportsAdapter : RecyclerView.Adapter<ReportsAdapter.ViewHolder>() {

    private var reports: List<Array<Any>> = ArrayList()

    fun submitList(reports: List<Array<Any>>) {
        this.reports = reports

        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]

        val reportText = report[0] as String
        val number = (report[1] as Double).toInt()

        holder.text.text = String.format(holder.text.context.getString(R.string.userReportFormat, number, reportText))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val text = TextView(parent.context)
        text.setTextColor(ContextCompat.getColor(parent.context, R.color.secondary_text_color))

        return ViewHolder(text)
    }

    override fun getItemCount() = reports.size


    inner class ViewHolder(val text: TextView) : RecyclerView.ViewHolder(text)
}