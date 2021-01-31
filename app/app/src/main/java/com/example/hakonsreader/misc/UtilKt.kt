package com.example.hakonsreader.misc

import android.widget.ImageView
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.Image
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.enums.ShowNsfwPreview
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso

/**
 * Loads an image from a network URL with a preferred and a backup URL. The backup URL will only
 * be loaded if the preferred image is not cached
 *
 * @param preferredUrl The preferred URL to load, which will only be loaded if it is
 * in cache
 * @param backupUrl The backup URL that will be loaded if [preferredUrl] is not cached
 * @param into The ImageView to load the image into
 */
fun Picasso.loadIf(preferredUrl: String?, backupUrl: String?, into: ImageView) {
    this.load(preferredUrl).networkPolicy(NetworkPolicy.OFFLINE).into(into, object : Callback {
        override fun onSuccess() {
            // Not implemented
        }

        override fun onError(e: Exception?) {
            this@loadIf.load(backupUrl).into(into)
        }
    })
}

/**
 * Class for holding image URL variants for a [RedditPost]
 *
 * @param normal The URL pointing to the normal URL. If this is null, no image was found
 * @param nsfw The URL pointing to the NSFW image to use. If this is null, no image should be shown
 * as either chosen by the user, or because no image was found
 * @param spoiler The URL pointing to the spoiler image to use. If this is null, no image was found and no image
 * should be used
 */
data class PostImageVariants(var normal: String?, var nsfw: String?, var spoiler: String?)

/**
 * Gets image variants for a reddit post
 *
 * @param post The post to get images for
 * @return A [PostImageVariants] that holds the images to use for if the post is normal, nsfw, or spoiler
 */
fun getImageVariantsForRedditPost(post: RedditPost) : PostImageVariants {
    return PostImageVariants(getNormal(post), getNsfw(post), getObfuscated(post))
}

/**
 * Gets the normal
 */
private fun getNormal(post: RedditPost) : String? {
    val screenWidth = App.get().screenWidth
    var imageUrl: String? = null

    post.getPreviewImages()?.forEach {
        if (it.width <= screenWidth) {
            imageUrl = it.url
        }
    }

    return imageUrl
}

/**
 * Gets the NSFW image to use for a post
 *
 * @return A URL pointing to the image to use for a post, depending on [App.showNsfwPreview]. If this is null
 * then no image should be shown ([ShowNsfwPreview.NO_IMAGE])
 */
private fun getNsfw(post: RedditPost) : String? {
    return when (App.get().showNsfwPreview()) {
        ShowNsfwPreview.NORMAL -> getNormal(post)
        ShowNsfwPreview.BLURRED -> getObfuscated(post)
        ShowNsfwPreview.NO_IMAGE-> null
    }
}

/**
 * Gets an obfuscated image URL for a post
 *
 * @return A URL pointing to an obfuscated image, or null if no image is available
 */
private fun getObfuscated(post: RedditPost) : String? {
    val obfuscatedPreviews: List<Image>? = post.getObfuscatedPreviewImages()

    return if (obfuscatedPreviews?.isNotEmpty() == true) {
        // Obfuscated previews that are high res are still fairly showing sometimes, so
        // get the lowest quality one as that will not be very easy to tell what it is
        obfuscatedPreviews[0].url!!
    } else {
        null
    }
}