package com.example.hakonsreader.views.preferences.multicolor

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.recyclerview.widget.ItemTouchHelper
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

        /**
         * The key used in [onSaveInstanceState] to save the current colors
         */
        private const val SAVE_STATE_COLORS = "savedColors"


        fun newInstance(key: String) = MultiColorFragCompat().apply {
            arguments = Bundle(1).apply {
                putString(ARG_KEY, key)
            }
        }

        /**
         * Gets parsed colors from a SharedPreferences, or default values if the value is empty or not set
         *
         * @param preferences The preferences to look in
         * @param key The key the value is stored with
         *
         * @return A list of hex colors. Note that the elements in this list does not include a "#" at the start
         */
        fun getColors(preferences: SharedPreferences, key: String) : List<String> {
            val value = preferences.getString(key, null)
            return if (value.isNullOrEmpty()) {
                listOf("FF18A5FD", "FF048E14", "FF9516A3", "FFB14902")
            } else {
                parseStoredValue(value)
            }
        }

        /**
         * Parses the stored value into a list of hex strings
         */
        private fun parseStoredValue(value: String) : List<String> {
            return value.split(",")
        }
    }

    private var _binding: PreferenceMultiColorBinding? = null
    private val binding get() = _binding!!

    private var savedColors: ArrayList<String>? = null


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedColors = savedInstanceState?.getStringArrayList(SAVE_STATE_COLORS)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateDialogView(context: Context?): View {
        _binding = PreferenceMultiColorBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)

        binding.colors.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ColorAdapter().apply {
            val colors = savedColors ?: getColors(preference.sharedPreferences, preference.key).toMutableList()
            submitList(colors)
            binding.colors.adapter = this
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.START or ItemTouchHelper.END
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition

                Collections.swap(adapter.colors, from, to)
                adapter.notifyItemMoved(from, to)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                adapter.removeColor(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(binding.colors)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.colors?.adapter.let {
            outState.putStringArrayList(SAVE_STATE_COLORS, (it as ColorAdapter).colors as ArrayList<String>)
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
     * Creates a parsed value of a list of hex colors that can be used to store the value
     */
    private fun createParsedValue(colors: List<String>) : String {
        // Parse the list as "color1,color2,color3"
        return buildString {
            colors.forEachIndexed { index, value ->
                append(value)
                if (index + 1 != colors.size) {
                    append(",")
                }
            }
        }
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

        /**
         * Updates the color at a given position
         *
         * @param position The position to update
         * @param hex The updated value
         */
        fun updateColor(position: Int, hex: String) {
            colors.removeAt(position)
            colors.add(position, hex)
            notifyItemChanged(position)
        }

        /**
         * Removes a color by a position
         *
         * @param position The position to remove
         */
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