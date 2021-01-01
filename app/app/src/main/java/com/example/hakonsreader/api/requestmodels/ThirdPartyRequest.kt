package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.model.ImgurAlbum
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.service.GfycatService
import com.example.hakonsreader.api.service.ImgurService
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

        loadImgurAlbums(postsWithImgurAlbum)
        loadGfycatGifs(postsWithGfycat)
        loadRedgifGifs(postsWithRedgif)
    }

    /**
     * Looks for any posts that link to Imgur albums and loads the individual items so they're
     * treated as a Reddit gallery
     *
     * @param posts The posts to load albums for
     */
    fun loadImgurAlbums(posts: List<RedditPost>) {
        if (imgurApi == null) {
            return
        }

        for (postWithImgurAlbum in posts) {
            try {
                // android.Uri I miss you :(
                val uri = URI(postWithImgurAlbum.url)

                val paths = uri.path.split("/".toRegex()).toTypedArray()
                val albumHash = paths[paths.size - 1]

                val response: Response<ImgurAlbum> = imgurApi.loadImgurAlbum(albumHash).execute()
                val album = response.body()

                if (!response.isSuccessful || album == null) {
                    return
                }

                val images = album.images
                postWithImgurAlbum.galleryImages = images
            } catch (e: URISyntaxException) {
                // This really should never happen but worst case is the album would be loaded as a link
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadGfycatGifs(posts: List<RedditPost>) {
        posts.forEach {
            try {
                val uri = URI(it.url)
                val paths = uri.path.split("/".toRegex()).toTypedArray()

                // Example URL: https://gfycat.com/tartcrazybelugawhale-adventures-confused-chilling-sabrina-kiernan-idea
                // "tartcrazybelugawhale" is the ID of the gif
                val id = paths.last().split("-").first()

                val gif = gfycatApi.gfycat(id).body()
                it.thirdPartyObject = gif
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadRedgifGifs(posts: List<RedditPost>) {
        posts.forEach {
            try {
                val uri = URI(it.url)
                val paths = uri.path.split("/".toRegex()).toTypedArray()

                // Example URL: https://gfycat.com/tartcrazybelugawhale-adventures-confused-chilling-sabrina-kiernan-idea
                // "tartcrazybelugawhale" is the ID of the gif
                val id = paths.last().split("-").first()

                val gif = gfycatApi.redgifs(id).body()
                it.thirdPartyObject = gif
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}