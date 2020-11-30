package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.model.Image;
import com.example.hakonsreader.api.model.ImgurAlbum;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.service.ImgurService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Response;

/**
 * Request model for various requests towards the Imgur API
 */
public class ImgurRequest {

    private final ImgurService api;

    public ImgurRequest(ImgurService api) {
        this.api = api;
    }

    /**
     * Note: This function uses the main thread for network operations and should be called from
     * a background thread.
     *
     * Looks for any posts that link to Imgur albums and loads the individual items so they're
     * treated as a Reddit gallery
     *
     * @param posts The posts to load albums for
     */
    public void loadAlbums(List<RedditPost> posts) {
        List<RedditPost> postsWithImgurAlbum = posts.stream()
                .filter(post -> post.getUrl().matches("https://imgur.com/a/.+"))
                .collect(Collectors.toList());

        for (RedditPost postWithImgurAlbum : postsWithImgurAlbum) {
            try {
                // android.Uri I miss you :(
                URI uri = new URI(postWithImgurAlbum.getUrl());

                String[] paths = uri.getPath().split("/");
                String albumHash = paths[paths.length - 1];

                Response<ImgurAlbum> response = api.loadImgurAlbum(albumHash).execute();
                ImgurAlbum album = response.body();
                if (!response.isSuccessful() || album == null) {
                    return;
                }

                List<Image> images = album.getImages();
                postWithImgurAlbum.setGalleryImages(images);
            } catch (URISyntaxException | IOException e) {
                // This really should never happen but worst case is the album would be loaded as a link
                e.printStackTrace();
            }
        }
    }
}
