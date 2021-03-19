package com.example.hakonsreader.views.preferences.multicolor

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.preference.DialogPreference
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.R

class MultiColorPreference(context: Context, attributeSet: AttributeSet) : DialogPreference(context, attributeSet) {
    override fun getSummary(): CharSequence {
        val colorCount = MultiColorFragCompat.getColors(sharedPreferences, key).size
        return context.resources.getQuantityString(R.plurals.multiColorPreferenceSummary, colorCount, colorCount)
    }
}