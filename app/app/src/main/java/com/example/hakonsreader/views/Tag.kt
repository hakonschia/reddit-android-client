package com.example.hakonsreader.views

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.databinding.TagBinding
import com.squareup.picasso.Picasso

class Tag @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding: TagBinding = TagBinding.inflate(LayoutInflater.from(context), this, true)
    private var textColor = 0
    private var textColorOverridden = false

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.Tag, 0, 0)
        try {
            val defaultTextColor = "#000000"
            textColor = a.getColor(R.styleable.Tag_textColor, Color.parseColor(defaultTextColor))
            val defaultFillColor = "#EAEAEA"
            val fillColor = a.getColor(R.styleable.Tag_fillColor, Color.parseColor(defaultFillColor))
            setFillColor(fillColor)
            val text = a.getString(R.styleable.Tag_text)
            if (text != null) {
                addText(text)
            } else {
                if (isInEditMode) {
                    addText("FaZe Clan fan")
                }
            }
        } finally {
            a.recycle()
        }

        // With elevation the card will have a weird looking shadow (only visible on light mode)
        binding.cardView.cardElevation = 0f
    }

    /**
     * Clears all views from the tag
     */
    fun clear() {
        binding.tags.removeAllViews()
    }

    /**
     * Adds a view to the flair
     *
     *
     * If the tag has multiple views ensure they are added in the correct order
     *
     *
     * Padding is added to the view
     */
    fun add(view: View) {
        view.setPadding(5, 0, 5, 1)
        binding.tags.addView(view)
    }

    /**
     * Sets the fill/background color of the tag
     *
     * @param color The color resource ID
     */
    fun setFillColor(color: Int) {
        binding.cardView.setCardBackgroundColor(color)
    }

    /**
     * Sets the fill/background color of the tag
     *
     * @param hexColor A hex string representing a color. Note: If this is set to "transparent"
     * then the text color will be overridden and always be set to the current themes
     * text color
     */
    fun setFillColor(hexColor: String) {
        if (hexColor == "transparent") {
            // Getting the transparent value out of a color is apparently harder (if there is no transparent
            // it still returns 255? so set it here only, not in setFillColor(int))
            setFillColor(ContextCompat.getColor(context, R.color.background_with_alpha))
            textColor = ContextCompat.getColor(context, R.color.text_color)
            textColorOverridden = true
            // The elevation shadow will still be visible on light backgrounds
            binding.cardView.cardElevation = 0f
        } else {
            setFillColor(Color.parseColor(hexColor))
        }
    }

    /**
     * Sets the text color for the tag. If [Tag.setFillColor] is set with "transparent"
     * then this will override the text color with the themes text color
     *
     * @param textColor The color to set for the text
     */
    fun setTextColor(textColor: Int) {
        if (!textColorOverridden) {
            this.textColor = textColor
        }
    }

    /**
     * Adds text to the tag
     *
     * Note the text color must be set with [Tag.setTextColor] before this is called
     *
     * @param text The text to add. If this is empty nothing is added to the tag
     */
    fun addText(text: String) {
        if (text.isEmpty()) {
            return
        }

        // If the last item in the tags is a TextView, add the text to that view instead
        // of creating a new view
        val tagsAdded = binding.tags.childCount
        if (tagsAdded > 0) {
            val last = binding.tags.getChildAt(tagsAdded - 1)
            if (last is TextView) {
                last.append(" $text")
                return
            }
        }

        TextView(context).run {
            this.text = text
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.tagTextSize))
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
            add(this)
        }
    }

    /**
     * Adds an image to the tag
     *
     * @param imageURL The URL to the image
     */
    fun addImage(imageURL: String?) {
        val size = context.resources.getDimension(R.dimen.tagIconSize).toInt()

        ImageView(context).run {
            Picasso.get()
                    .load(imageURL)
                    .resize(size, size)
                    .into(this)
            add(this)
        }
    }
}