package com.example.hakonsreader.views.preferences.multicolor

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
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


/**
 * Preference dialog for selecting multiple colors
 */
class MultiColorFragCompat : PreferenceDialogFragmentCompat() {
    companion object {
        private const val TAG = "MultiColorFragCompat"

        /**
         * The key used in [onSaveInstanceState] to save the current colors
         */
        private const val SAVE_STATE_COLORS = "savedColors"

        /**
         * The key used in [onSaveInstanceState] to save the class name [colorPickerState] is currently
         */
        private const val SAVE_STATE_COLOR_PICKER_STATE_NAME = "colorPickerStateName"

        /**
         * The key used in [onSaveInstanceState] to save the current hex color of the active [ColorPicker],
         * if there is one
         */
        private const val SAVE_STATE_COLOR_PICKER_COLOR = "colorPickerColor"

        /**
         * The key used in [onSaveInstanceState] to save the color position currently being edited, if
         * [colorPickerState] is [ColorPickerState.Editing]
         */
        private const val SAVE_STATE_COLOR_PICKER_EDITING_POSITION = "colorPickerStateEditPos"


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

    /**
     * Class representing the state of the [ColorPicker]
     */
    private sealed class ColorPickerState {
        /**
         * No [ColorPicker] is shown
         */
        object Nothing : ColorPickerState()

        /**
         * A color is being added
         *
         * @param colorPicker The color picker shown
         */
        class Adding(val colorPicker: ColorPicker) : ColorPickerState()

        /**
         * A color is being edited
         *
         * @param position The position of the color being edited
         * @param colorPicker The color picker shown
         */
        class Editing(val position: Int, val colorPicker: ColorPicker) : ColorPickerState()
    }

    private var _binding: PreferenceMultiColorBinding? = null
    private val binding get() = _binding!!

    /**
     * The current state of the color picker
     */
    private var colorPickerState: ColorPickerState = ColorPickerState.Nothing

    /**
     * THe saved instance state
     */
    private var savedInstanceState: Bundle? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        this.savedInstanceState = savedInstanceState
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateDialogView(context: Context?): View {
        _binding = PreferenceMultiColorBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)

        val savedColors = savedInstanceState?.getStringArrayList(SAVE_STATE_COLORS)

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
                createAndShowColorPicker(context, adapter)
            }
        }

        savedInstanceState?.let {
            // Not sure why this is necessary, but if a color picker is being shown from the save state
            // it will appear under the dialog for the preference unless there is some sort of delay
            binding.root.post {
                restoreColorPickerState(it, adapter)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveColorPickerState(colorPickerState, outState)

        _binding?.colors?.adapter.let {
            outState.putStringArrayList(SAVE_STATE_COLORS, (it as ColorAdapter).colors as ArrayList<String>)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.colors.adapter = null

        // Ensure the color pickers don't leak
        colorPickerState = ColorPickerState.Nothing

        _binding = null
    }

    /**
     * Creates and shows a [ColorPicker].
     *
     * @param activity The activity to create the dialog with
     * @param adapter The adapter to notify when the color picker has chosen a color
     * @param startingColor Optionally, the starting hex color to use in the color picker (should not be
     * prefixed with "#")
     * @param editPosition Optionally, the position being edited (if a color is being edited)
     */
    private fun createAndShowColorPicker(activity: AppCompatActivity, adapter: ColorAdapter, startingColor: String? = null, editPosition: Int = -1) {
        // To let the user specify the alpha a starting value must be specified, so ensure it is
        val color = startingColor?.padStart(8, '0') ?: "FF000000"
        val parsedColor = Color.parseColor("#$color")

        ColorPicker(
                activity,
                Color.alpha(parsedColor),
                Color.red(parsedColor),
                Color.green(parsedColor),
                Color.blue(parsedColor)
        ).apply {
            setCallback { color ->
                val hex = createPaddedHexString(color)
                if (editPosition >= 0) {
                    adapter.updateColor(editPosition, hex)
                } else {
                    adapter.addColor(hex)
                }
            }

            setOnShowListener {
                colorPickerState = if (editPosition >= 0) {
                    ColorPickerState.Editing(editPosition, this)
                } else {
                    ColorPickerState.Adding(this)
                }
            }

            setOnDismissListener {
                colorPickerState = ColorPickerState.Nothing
            }

            enableAutoClose()

            show()
        }
    }

    /**
     * Creates a parsed value of a list of hex colors that can be used to store the value.
     *
     * @return A string with each hex color split with a ","
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

    /**
     * Creates a padded hex string that will always be 8 characters long to ensure that all color
     * parts (ARGB) are explicitly specified. This ensures that parsing hex strings back to colors wont
     * cause an unknown color exception
     *
     * @param color The color to convert to hex
     * @return A hex representation of [color] that will always be 8 characters long
     */
    private fun createPaddedHexString(color: Int) : String {
        return Integer.toHexString(color).toUpperCase(Locale.ROOT).padStart(8, '0')
    }

    /**
     * Saves the state of a [ColorPickerState] to a bundle. If the state holds a [ColorPicker] then it
     * will be dismissed after its state is saved
     *
     * @param state The state to save
     * @param outState The bundle to save the state to
     */
    private fun saveColorPickerState(state: ColorPickerState, outState: Bundle) {
        outState.putString(SAVE_STATE_COLOR_PICKER_STATE_NAME, state.javaClass.name)

        when (state) {
            is ColorPickerState.Adding -> {
                val color = createPaddedHexString(state.colorPicker.color)
                outState.putString(SAVE_STATE_COLOR_PICKER_COLOR, color)

                state.colorPicker.dismiss()
            }
            is ColorPickerState.Editing -> {
                val color = createPaddedHexString(state.colorPicker.color)
                outState.putString(SAVE_STATE_COLOR_PICKER_COLOR, color)
                outState.putInt(SAVE_STATE_COLOR_PICKER_EDITING_POSITION, state.position)

                state.colorPicker.dismiss()
            }
            ColorPickerState.Nothing -> {
                // Nothing to save
            }
        }
    }

    /**
     * Restores [ColorPickerState] by showing a new [ColorPicker] based on a saved state
     *
     * @param savedInstanceState The bundle with the saved state
     * @param adapter The adapter to use to add/update when the color picker has chosen a color
     */
    private fun restoreColorPickerState(savedInstanceState: Bundle, adapter: ColorAdapter) {
        when (savedInstanceState.getString(SAVE_STATE_COLOR_PICKER_STATE_NAME)) {
            ColorPickerState.Adding::class.java.name -> {
                val color = savedInstanceState.getString(SAVE_STATE_COLOR_PICKER_COLOR)

                createAndShowColorPicker(requireActivity() as AppCompatActivity, adapter, startingColor = color)
            }
            ColorPickerState.Editing::class.java.name -> {
                val pos = savedInstanceState.getInt(SAVE_STATE_COLOR_PICKER_EDITING_POSITION)
                val color = savedInstanceState.getString(SAVE_STATE_COLOR_PICKER_COLOR)

                createAndShowColorPicker(requireActivity() as AppCompatActivity, adapter, startingColor = color, editPosition = pos)
            }
        }
    }


    private inner class ColorAdapter : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
        var colors: MutableList<String> = ArrayList()
            private set

        /**
         * Submits a list of hex colors to the adapter
         *
         * @param colors The list of colors to display. These colors should be raw hex strings (no
         * "#" at the start)
         */
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
                        createAndShowColorPicker(
                                it.context as AppCompatActivity,
                                adapter = this@ColorAdapter,
                                startingColor = colors[adapterPosition],
                                editPosition = adapterPosition
                        )
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