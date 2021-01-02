package com.example.hakonsreader.api.requestmodels

import android.util.Log
import com.example.hakonsreader.api.model.ImgurAlbum
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.service.GfycatService
import com.example.hakonsreader.api.service.ImgurService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import kotlin.reflect.KSuspendFunction1

/**
 * Request model for communicating with third party services, such as Imgur and Gfycat
 */
class ThirdPartyRequest(private val imgurApi: ImgurService?, private val gfycatApi: GfycatService) {

    /**
     * Loads all third party contents for a list of posts.
     *
     * The following services will potentially be called:
     * 1. Imgur (for albums)
     * 2. Gfycat (for gifs)
     * 3. Redgifs (for gifs)
     *
     * @param posts The posts to load for
     * @param indexToSpawnNewCoroutine The index in [posts] at which to start spawning new Coroutine jobs
     * when making API calls. Every index before this number will run the API call on the same Coroutine
     * job that this function was called at, and afterwards each call is passed to a new job to offload
     * the loading on the original job. This makes it so the first posts will be guaranteed to have
     * the third party API calls made before the original job finishes. Default value is `5`
     */
    suspend fun loadAll(posts: List<RedditPost>, indexToSpawnNewCoroutine: Int = 5) {
        posts.forEachIndexed { index, post ->
            val func = when {
                post.domain == "gfycat.com" -> this::loadGfycatGif
                post.domain == "redgifs.com" -> this::loadRedgifGif
                post.url.matches("https://imgur.com/a/.+".toRegex()) -> this::loadImgurAlbum
                else -> null
            } ?: return@forEachIndexed

            // If we are at the start of the list, we want to call the third party API calls immediately
            // Otherwise, the posts might be shown to the user and have the content generated before
            // the API call is done, which causes the Reddit content to be shown, instead of the third party content
            // We want the others to be done on a different Coroutine as we don't want to wait for every API call
            // to be finished before "returning" the list since that can cause a very noticeable delay
            // with many API calls. Eg. go to the subreddit "nsfwgif" which has a lot of posts from Redgifs, it
            // can cause an extra delay of 10+ seconds
            if (index >= indexToSpawnNewCoroutine) {
                Log.d("ThirdPartyRequest", "loadAll: index = $index, launching new job")
                CoroutineScope(IO).launch {
                    func.invoke(post)
                }
            } else {
                Log.d("ThirdPartyRequest", "loadAll: index = $index, continuing job")
                func.invoke(post)
            }
        }
    }


    suspend fun loadImgurAlbum(post: RedditPost) {
        if (imgurApi == null) {
            return
        }

        try {
            // android.Uri I miss you :(
            val uri = URI(post.url)

            val paths = uri.path.split("/".toRegex()).toTypedArray()
            val albumHash = paths[paths.size - 1]

            val response: Response<ImgurAlbum> = imgurApi.loadImgurAlbum(albumHash).execute()
            val album = response.body()

            if (!response.isSuccessful || album == null) {
                return
            }

            val images = album.images
            post.galleryImages = images
            post.crossposts?.forEach {
                it.galleryImages = images
            }
        } catch (e: URISyntaxException) {
            // This really should never happen but worst case is the album would be loaded as a link
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun loadGfycatGif(post: RedditPost) {
        try {
            val uri = URI(post.url)
            val paths = uri.path.split("/".toRegex()).toTypedArray()

            // Example URL: https://gfycat.com/tartcrazybelugawhale-adventures-confused-chilling-sabrina-kiernan-idea
            // "tartcrazybelugawhale" is the ID of the gif. The "-" are not always present and might only include the ID
            val id = paths.last().split("-").first()

            val gif = gfycatApi.gfycat(id).body()?.gif

            // For some reason, gfycat sends back the mp4 url with http, so ensure it's https
            gif?.let {
                var url = it.mp4Url
                if (url[4] != 's') {
                    url = "https" + url.substring(4)
                    it.mp4Url = url
                }
            }

            post.thirdPartyObject = gif
            post.crossposts?.get(0)?.thirdPartyObject = gif
            post.crossposts?.forEach {
                it.thirdPartyObject = gif
            }
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun loadRedgifGif(post: RedditPost) {
        try {
            val uri = URI(post.url)
            val paths = uri.path.split("/".toRegex()).toTypedArray()

            // Example URL: https://redgifs.com/watch/lateplayfulgiraffe
            // "tartcrazybelugawhale" is the ID of the gif. The "-" are not always present and might only include the ID
            val id = paths.last().split("-").first()

            val gif = gfycatApi.redgifs(id).body()?.gif
            gif?.let {
                var url = it.mp4Url
                if (url[4] != 's') {
                    url = "https" + url.substring(4)
                    it.mp4Url = url
                }
            }

            post.thirdPartyObject = gif
            post.crossposts?.forEach {
                it.thirdPartyObject = gif
            }
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}