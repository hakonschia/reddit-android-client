package com.example.hakonsreader.views.preferences.multicolor

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.databinding.PreferenceListItemColorBinding
import com.example.hakonsreader.databinding.PreferenceMultiColorBinding
import com.pes.androidmaterialcolorpickerdialog.ColorPicker
import java.util.*
import kotlin.collections.ArrayList


class MultiColorFragCompat : PreferenceDialogFragmentCompat() {
    companion object {
        private const val TAG = "MultiColorFragCompat"

        fun newInstance(key: String) = MultiColorFragCompat().apply {
            arguments = Bundle(1).apply {
                putString(ARG_KEY, key)
            }
        }
    }

    private var _binding: PreferenceMultiColorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialogView(context: Context?): View {
        _binding = PreferenceMultiColorBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)

        val adapter = ColorAdapter().apply {
            // Sets are unordered, and we want the order to be the order in which the comments are colored
            // so we have to store it as a single string and parse it ourselves

            val value = preference.sharedPreferences.getString(requireArguments().getString(ARG_KEY), null)
            val colors = if (value.isNullOrEmpty()) {
                getDefaultColors()
            } else {
                parseStoredValue(value)
            }
            submitList(colors.toMutableList())
            binding.colors.adapter = this
        }
        binding.colors.layoutManager = LinearLayoutManager(requireContext())

        binding.addColor.setOnClickListener {
            val context = requireContext()
            if (context is AppCompatActivity) {
                ColorPicker(context, 255, 0, 0, 0).run {
                    setCallback { color ->
                        val hex = Integer.toHexString(color).toUpperCase(Locale.ROOT)
                        Log.d(TAG, hex)
                        adapter.addColor(hex)
                    }
                    // Automatically close when a color is chosen
                    enableAutoClose()
                    show()
                }
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            // Store new values
            val colors = (binding.colors.adapter as ColorAdapter).colors

            preference.sharedPreferences.edit()
                    .putString(requireArguments().getString(ARG_KEY), createParsedValue(colors))
                    .apply()
        }
    }

    /**
     * Gets a list of default colors to use if there is no value stored
     */
    // Should ideally use preference default values, but the values are technically a list but is
    // stored as one string, so dunno how that would work
    private fun getDefaultColors() = listOf("FF18A5FD", "FF048E14", "FF9516A3", "FFB14902")

    /**
     * Parses the stored value into a list of hex strings
     */
    private fun parseStoredValue(value: String) : List<String> {
        return value.split(",")
    }

    /**
     * Creates a parsed value of a list of hex colors that can be used to store the value
     */
    private fun createParsedValue(colors: List<String>) : String {
        val builder = StringBuilder("")
        colors.forEachIndexed { index, value ->
            builder.append(value)
            if (index + 1 != colors.size) {
                builder.append(",")
            }
        }
        return builder.toString()
    }


    private inner class ColorAdapter : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
        var colors: MutableList<String> = ArrayList()

        fun submitList(colors: MutableList<String>) {
            this.colors = colors
            notifyDataSetChanged()
        }

        /**
         * Adds a new color to the adapter
         *
         * @param hex The hex representation of the color. Note this should NOT include a "#" at the start
         */
        fun addColor(hex: String) {
            colors.add(hex)
            notifyItemInserted(colors.size - 1)
        }

        fun updateColor(position: Int, hex: String) {
            colors.removeAt(position)
            colors.add(position, hex)
            notifyItemChanged(position)
        }

        fun removeColor(position: Int) {
            colors.removeAt(position)
            notifyItemRemoved(position)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val color = colors[position]
            with(holder.binding) {
                hexColor.text = color
                colorPreview.setBackgroundColor(Color.parseColor("#$color"))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(PreferenceListItemColorBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
            ))
        }

        override fun getItemCount() = colors.size

        private inner class ViewHolder(val binding: PreferenceListItemColorBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val current = Color.parseColor("#${colors[adapterPosition]}")

                        ColorPicker(it.context as AppCompatActivity, Color.alpha(current), Color.red(current), Color.green(current), Color.blue(current)).run {
                            setCallback { color ->
                                val hex = Integer.toHexString(color).toUpperCase(Locale.ROOT)
                                updateColor(adapterPosition, hex)
                            }
                            // Automatically close when a color is chosen
                            enableAutoClose()
                            show()
                        }
                    }
                }
                binding.removeColor.setOnClickListener {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        removeColor(adapterPosition)
                    }
                }
            }
        }
    }
}