package com.example.hakonsreader.views.util

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
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
    val activity = context as Activity

    // Send some data like what sub it is etc etc so it knows what to load
    Intent(context, SubredditActivity::class.java).run {
        putExtra(SubredditActivity.EXTRAS_SUBREDDIT_KEY, subreddit)
        activity.startActivity(this)

        // Slide the activity in
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
    val activity = context as Activity

    // Send some data like what sub it is etc etc so it knows what to load
    Intent(context, ProfileActivity::class.java).run {
        putExtra(ProfileActivity.EXTRAS_USERNAME_KEY, username)
        activity.startActivity(this)

        // Slide the activity in
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}

/**
 * Opens an image in fullscreen with a SharedElementTransition
 *
 * @param view The view to use. This should be an ImageView, but is defined as a View to be compatible
 * with usage in XML layouts
 * @param imageUrl The URL to the image
 * @param cache True to cache the image once opened
 * @param useBitmapFromView If set to true [ImageActivity.BITMAP] will be set to the bitmap [view] holds,
 * if [view] is an ImageView with a BitmapDrawable
 */
fun openImageInFullscreen(view: View, imageUrl: String?, cache: Boolean, useBitmapFromView: Boolean) {
    val context = view.context
    val activity = context as Activity

    Intent(context, ImageActivity::class.java).run {
        if (useBitmapFromView && view is ImageView) {
            ImageActivity.BITMAP = view.drawable?.toBitmap()
        }

        putExtra(ImageActivity.EXTRAS_IMAGE_URL, imageUrl)
        putExtra(ImageActivity.EXTRAS_CACHE_IMAGE, cache)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, context.getString(R.string.transition_image_fullscreen))
        activity.startActivity(this, options.toBundle())
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