package com.example.hakonsreader.views.util

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.ImageActivity
import com.example.hakonsreader.activities.ProfileActivity
import com.example.hakonsreader.activities.SubredditActivity

/**
 * Opens an activity with the selected subreddit
 *
 * @param view The view itself is ignored, but this cannot be null as the context is needed
 * @param subreddit The subreddit to open
 */
fun openSubredditInActivity(view: View, subreddit: String) {
    val context = view.context

    // Send some data like what sub it is etc etc so it knows what to load
    Intent(context, SubredditActivity::class.java).run {
        putExtra(SubredditActivity.EXTRAS_SUBREDDIT_KEY, subreddit)
        context.startActivity(this)

        // Slide the activity in
        if (context is AppCompatActivity) {
            context.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}

/**
 * Opens an activity to show a users profile
 *
 * @param view The view itself is ignored, but this cannot be null as the context is needed
 * @param username The username of the profile to open
 */
fun openProfileInActivity(view: View, username: String?) {
    val context = view.context

    // Send some data like what sub it is etc etc so it knows what to load
    Intent(context, ProfileActivity::class.java).run {
        putExtra(ProfileActivity.EXTRAS_USERNAME_KEY, username)
        context.startActivity(this)

        // Slide the activity in
        if (context is AppCompatActivity) {
            context.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}

/**
 * Opens an image in fullscreen. If the context of [view] is an activity then the image
 * is opened with a SharedElementTransition
 *
 * @param view The view to use. This should be an ImageView, but is defined as a View to be compatible
 * with usage in XML layouts
 * @param imageUrl The URL to the image
 * @param cache True to cache the image once opened
 * @param useBitmapFromView If set to true [ImageActivity.BITMAP] will be set to the bitmap [view] holds,
 * if [view] is an ImageView with a BitmapDrawable. If the ImageView holds the drawable
 * [R.drawable.ic_image_not_supported_200dp] then the bitmap will not be used.
 */
fun openImageInFullscreen(view: View, imageUrl: String?, cache: Boolean, useBitmapFromView: Boolean) {
    val context = view.context

    Intent(context, ImageActivity::class.java).run {
        if (useBitmapFromView && view is ImageView) {
            val isDrawableImageErrorDrawable = view
                .drawable
                ?.constantState
                ?.equals(ContextCompat.getDrawable(
                    view.context,
                    R.drawable.ic_image_not_supported_200dp
                )?.constantState)

            // Only use the bitmap if it isn't an "Error loading image" bitmap
            if (isDrawableImageErrorDrawable == false) {
                ImageActivity.BITMAP = view.drawable?.toBitmap()
            }
        }

        putExtra(ImageActivity.EXTRAS_IMAGE_URL, imageUrl)
        putExtra(ImageActivity.EXTRAS_CACHE_IMAGE, cache)

        if (context is AppCompatActivity) {
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(context, view, context.getString(R.string.transition_image_fullscreen))
            context.startActivity(this, options.toBundle())
        } else {
            context.startActivity(this)
        }
    }
}

/**
 * Function that can be used as onLongClick that displays a toast with the views
 * `contentDescription`
 *
 * @param view The view to display the description for
 * @return True (event consumed)
 */
fun showToastWithContentDescription(view: View): Boolean {
    val description = view.contentDescription
    if (description != null) {
        val desc = view.contentDescription.toString()
        if (desc.isNotEmpty()) {
            Toast.makeText(view.context, description, Toast.LENGTH_SHORT).show()
        }
    }
    return true
}