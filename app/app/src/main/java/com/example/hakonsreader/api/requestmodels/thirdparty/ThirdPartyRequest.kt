package com.example.hakonsreader.api.requestmodels.thirdparty

import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.service.thirdparty.GfycatService
import com.example.hakonsreader.api.service.thirdparty.ImgurService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Request model for communicating with third party services, such as Imgur and Gfycat
 */
class ThirdPartyRequest(private val imgurApi: ImgurService?, private val gfycatApi: GfycatService) {

    /**
     * Loads all third party contents for a list of posts.
     *
     * The following services will potentially be called:
     * - Imgur (for albums)
     * - Imgur (for gifs/mp4s)
     * - Gfycat (for gifs)
     * - Redgifs (for gifs)
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
            // If we are at the start of the list, we want to call the third party API calls immediately
            // Otherwise, the posts might be shown to the user and have the content generated before
            // the API call is done, which causes the Reddit content to be shown, instead of the third party content
            // We want the others to be done on a different Coroutine as we don't want to wait for every API call
            // to be finished before "returning" the list since that can cause a very noticeable delay
            // with many API calls. Eg. go to the subreddit "nsfwgif" which has a lot of posts from Redgifs, it
            // can cause an extra delay of 10+ seconds
            if (index >= indexToSpawnNewCoroutine) {
                CoroutineScope(IO).launch {
                    loadAll(post)
                }
            } else {
                loadAll(post)
            }
        }
    }

    /**
     * Loads all third party contents for a post. This will run on the current coroutine job
     *
     * The following services will potentially be called:
     * - Imgur (for albums)
     * - Imgur (for gifs/mp4s)
     * - Gfycat (for gifs)
     * - Redgifs (for gifs)
     *
     * @param post The post to load for
     */
    suspend fun loadAll(post: RedditPost) {
        val func = when {
            post.domain == "gfycat.com" -> this::loadGfycatGif
            post.domain == "redgifs.com" -> this::loadRedgifGif
            // We can use http for this as the http URL itself isn't loaded, it is just used as an identifier
            post.url.matches("http(s)?://(m\\.)?imgur\\.com/a/.+".toRegex()) -> this::loadImgurAlbum
            post.url.matches("http(s)?://([im])\\.imgur\\.com/.+(\\.(gif(v)?|mp4))".toRegex()) -> this::loadImgurGif
            else -> null
        } ?: return

        func.invoke(post)
    }

    /**
     * Loads content for imgur albums
     *
     * @param post The post to load the album for
     */
    suspend fun loadImgurAlbum(post: RedditPost) {
        imgurApi ?: return

        try {
            // android.Uri I miss you :(
            val uri = URI(post.url)

            val paths = uri.path.split("/".toRegex()).toTypedArray()
            val albumHash = paths[paths.size - 1]

            val response = imgurApi.getAlbum(albumHash)
            val album = response.body()

            if (!response.isSuccessful || album == null) {
                return
            }

            post.thirdPartyObject = album
            post.crossposts?.forEach {
                it.thirdPartyObject = album
            }
        } catch (e: URISyntaxException) {
            // This really should never happen but worst case is the album would be loaded as a link
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Loads content for Imgur gifs
     *
     * @param post The post to load the gif for
     */
    suspend fun loadImgurGif(post: RedditPost) {
        imgurApi ?: return

        try {
            val uri = URI(post.url)

            // Example url: https://i.imgur.com/cAu4x9y.gifv
            // id = cAu4x9y
            val paths = uri.path.split("/".toRegex()).toTypedArray()
            val id = paths.last().split(".").first()

            val gif = imgurApi.getImage(id).body()?.gif

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


    /**
     * Loads content for Gfycat gifs. This will only work for gifs hosted on Gfycat, for Redgifs
     * see [loadRedgifGif]
     *
     * @param post The post to load the gif for
     * @see loadRedgifGif
     */
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
            post.crossposts?.forEach {
                it.thirdPartyObject = gif
            }
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Loads content for Redgif gifs. This will only work for gifs hosted on Redgif, for Gfycat
     * see [loadGfycatGif]
     *
     * @param post The post to load the gif for
     * @see loadGfycatGif
     */
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