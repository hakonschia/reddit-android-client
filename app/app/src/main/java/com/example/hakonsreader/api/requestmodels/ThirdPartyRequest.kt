package com.example.hakonsreader.api.requestmodels

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

/**
 * Request model for communicating with third party services, such as Imgur and Gfycat
 */
class ThirdPartyRequest(private val imgurApi: ImgurService?, private val gfycatApi: GfycatService) {

    suspend fun load(posts: List<RedditPost>) {
        val postsWithImgurAlbum = posts.filter { post -> post.url.matches("https://imgur.com/a/.+".toRegex()) }
        val postsWithGfycat = posts.filter { post -> post.domain == "gfycat.com" }
        val postsWithRedgif = posts.filter { post -> post.domain == "redgifs.com" }

        val before = System.currentTimeMillis()
        loadImgurAlbums(postsWithImgurAlbum)
        loadGfycatGifs(postsWithGfycat)
        loadRedgifGifs(postsWithRedgif)

        val elapsed = (System.currentTimeMillis() - before) / 1000f
        println("Loading extras took $elapsed seconds")
    }

    /**
     * Looks for any posts that link to Imgur albums and loads the individual items so they're
     * treated as a Reddit gallery
     *
     * @param posts The posts to load albums for
     */
    suspend fun loadImgurAlbums(posts: List<RedditPost>) {
        if (imgurApi == null) {
            return
        }

        for (postWithImgurAlbum in posts) {
            try {
                CoroutineScope(IO).launch {
                    // android.Uri I miss you :(
                    val uri = URI(postWithImgurAlbum.url)

                    val paths = uri.path.split("/".toRegex()).toTypedArray()
                    val albumHash = paths[paths.size - 1]

                    val response: Response<ImgurAlbum> = imgurApi.loadImgurAlbum(albumHash).execute()
                    val album = response.body()

                    if (!response.isSuccessful || album == null) {
                        return@launch
                    }

                    val images = album.images
                    postWithImgurAlbum.galleryImages = images
                }
            } catch (e: URISyntaxException) {
                // This really should never happen but worst case is the album would be loaded as a link
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadGfycatGifs(posts: List<RedditPost>) {
        posts.forEach { post ->
            try {
                // This function has to be in a Coroutine already, but if there are many gfycat posts
                // it will be noticeably slow to load every post, since every post has to make an API call
                // and finish that before the next one can load, so this will make every one launch without waiting
                // I'm not sure if this can cause issues as
                    // 1) How do you know when we're finished?
                    // 2) If the first items shown are fairly slow at making the API call, then the posts
                    // might load in the list, which might make this be ignored. Should probably test with a slow
                    // connection at least and see what happens and how to solve that issue
                    // For gifs it might be better to not make this API call until we actually want to play the gif
                CoroutineScope(IO).launch {
                    val uri = URI(post.url)
                    val paths = uri.path.split("/".toRegex()).toTypedArray()

                    // Example URL: https://gfycat.com/tartcrazybelugawhale-adventures-confused-chilling-sabrina-kiernan-idea
                    // "tartcrazybelugawhale" is the ID of the gif
                    val id = paths.last().split("-").first()

                    val gif = gfycatApi.gfycat(id).body()?.gif

                    // For some reason Gfycat sometimes give back a http url, which can cause issues
                    // when playing, since cleartext might not be allowed
                    gif?.let {
                        var url = it.mp4Url
                        if (url[4] != 's') {
                            url = "https" + url.substring(4)
                            it.mp4Url = url
                        }
                    }

                    post.thirdPartyObject = gif
                }
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadRedgifGifs(posts: List<RedditPost>) {
        posts.forEach { post ->
            try {
                CoroutineScope(IO).launch {
                    val uri = URI(post.url)
                    val paths = uri.path.split("/".toRegex()).toTypedArray()

                    // Example URL: https://gfycat.com/tartcrazybelugawhale-adventures-confused-chilling-sabrina-kiernan-idea
                    // "tartcrazybelugawhale" is the ID of the gif
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
                }
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}