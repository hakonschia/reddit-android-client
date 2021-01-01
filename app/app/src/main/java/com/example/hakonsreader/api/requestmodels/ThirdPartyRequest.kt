package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.model.ImgurAlbum
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.service.ImgurService
import retrofit2.Response
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.stream.Collectors

/**
 * Request model for communicating with third party services, such as Imgur and Gfycat
 */
class ThirdPartyRequest(private val imgurApi: ImgurService?) {

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

        val postsWithImgurAlbum = posts.stream()
                .filter { post: RedditPost -> post.url.matches("https://imgur.com/a/.+".toRegex()) }
                .collect(Collectors.toList())

        for (postWithImgurAlbum in postsWithImgurAlbum) {
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
}