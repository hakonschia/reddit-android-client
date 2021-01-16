package com.example.hakonsreader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.api.interfaces.AwardableListing
import com.example.hakonsreader.api.model.RedditAward
import com.example.hakonsreader.databinding.AwardLayoutBinding
import com.example.hakonsreader.fragments.bottomsheets.ShowAwardBottomSheet
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.misc.loadIf
import com.squareup.picasso.Picasso

/**
 * Layout that wraps around Reddit awards and shows all
 */
class AwardLayout : FrameLayout {
    companion object {
        private const val TAG = "AwardLayout"
    }

    private val binding: AwardLayoutBinding = AwardLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * The awardable listing to display in this layout
     */
    var listing: AwardableListing? = null
        set(value) {
            field = value
            updateView()
        }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private fun updateView() {
        // Sort awards by descending price (so the most expensive awards are shown first)
        val sortedAwards = listing?.awardings?.sortedByDescending { award -> award.price } ?: return

        val awardImageTopBottomMargin = Util.dpToPixels(2f, resources)
        val awardImageStartMargin = Util.dpToPixels(6f, resources)
        val count = getTotalAwardsCount()

        // If the listing is updated the old views will still be there
        binding.container.removeAllViews()

        sortedAwards.forEach { award ->
            // 48 seems to fit fine. This could also be a setting
            // The sizes are: 16, 32, 48, 64, 128 (although some urls actually point to a higher res
            // image even though it says in the image object it is lower)
            // The bottom sheet will show a larger version of the image, so if that has been loaded already
            // it will be cached and we can use that instead to save some networking (and show a higher res image)
            val preferredUrl = award.resizedIcons?.find { image -> image.height == ShowAwardBottomSheet.IMAGE_SIZE }?.url
            val backupUrl = award.resizedIcons?.find { image -> image.height == 48 }?.url

            // TODO some images are animated gifs, which would be cool to have (although as a setting)
            //  Although I don't know how to check if it's animated before loading the image
            ImageView(context).apply {
                layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    // If we only have 1 award we don't want to margin at the start as it looks weird
                    // since there won't be a "2 awards" text to the left of it
                    marginStart = if (count != 1) {
                        awardImageStartMargin
                    } else 0

                    topMargin = awardImageTopBottomMargin
                    bottomMargin = awardImageTopBottomMargin
                }

                adjustViewBounds = true
                setOnClickListener { showAwardDescription(award) }

                Picasso.get().loadIf(preferredUrl, backupUrl, this)
                binding.container.addView(this)
            }

            // Only show "2 awards" text if there are more than 1 award
            // It feels kind of weird to have it with only 1, and then I don't have to bother with string plurals :)
            if (award.count > 1) {
                TextView(context).apply {
                    text = award.count.toString()
                    setTextColor(ContextCompat.getColor(context, R.color.secondary_text_color))
                    setOnClickListener { showAwardDescription(award) }

                    binding.container.addView(this)
                }
            }
        }

        binding.amountOfAwards = count
    }

    private fun getTotalAwardsCount() : Int {
        var count = 0
        listing?.awardings?.forEach { count += it.count }
        return count
    }

    /**
     * Shows a bottom sheet displaying information about an award
     *
     * @param award The award to display
     */
    private fun showAwardDescription(award: RedditAward) {
        val awardBottomSheet = ShowAwardBottomSheet()
        awardBottomSheet.award = award
        awardBottomSheet.show((context as AppCompatActivity).supportFragmentManager, "Award bottom sheet")
    }
}