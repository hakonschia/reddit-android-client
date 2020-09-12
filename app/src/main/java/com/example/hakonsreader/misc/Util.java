package com.example.hakonsreader.misc;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.PostContentImage;
import com.example.hakonsreader.views.PostContentLink;
import com.example.hakonsreader.views.PostContentText;
import com.example.hakonsreader.views.PostContentVideo;

public class Util {


    /**
     * Generates content view for a post
     *
     * @param post The post to generate for
     * @return A view with the content of the post
     */
    public static View generatePostContent(RedditPost post, Context context) {
        switch (post.getPostType()) {
            case IMAGE:
                return new PostContentImage(context, post);

            case VIDEO:
                return new PostContentVideo(context, post);

            case RICH_VIDEO:
                // Links such as youtube, gfycat etc are rich video posts
                return null;

            case LINK:
                return new PostContentLink(context, post);

            case TEXT:
                return new PostContentText(context, post);

            default:
                return null;
        }
    }

    /**
     * Generates the content for video posts
     *
     * @param post The post to generate content for
     * @return A VideoView
     */
    public static VideoView generateVideoContent(RedditPost post, Context context) {
        VideoView videoView = new VideoView(context);

        // TODO width should be maximum this to not go outside of the screen
        videoView.setLayoutParams(new ViewGroup.LayoutParams(post.getVideoWidth(), post.getVideoHeight()));
        videoView.setVideoURI(Uri.parse(post.getVideoUrl()));

        return videoView;
    }

}
