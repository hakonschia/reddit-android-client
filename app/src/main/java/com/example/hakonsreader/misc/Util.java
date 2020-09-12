package com.example.hakonsreader.misc;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

import com.example.hakonsreader.MainActivity;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.PostContentLink;
import com.squareup.picasso.Picasso;

public class Util {

    /**
     * Generates content view for a post
     *
     * @param post The post to generate for
     * @param context The context in which to generate the view
     * @return A view with the content of the post
     */
    public static View generatePostContent(RedditPost post, Context context) {
        // Add the content
        View view = null;

        PostType postType = post.getPostType();

        switch (postType) {
            case IMAGE:
                view = Util.generateImageContent(post, context);
                break;

            case VIDEO:
                view = Util.generateVideoContent(post, context);
                break;

            case RICH_VIDEO:
                // Links such as youtube, gfycat etc are rich video posts
                break;

            case LINK:
                view = Util.generateLinkContent(post, context);
                break;

            case TEXT:
                // Do nothing special for text posts
                break;
        }

        return view;
    }

    /**
     * Generates the content for image posts
     *
     * @param post The post to generate content for
     * @return An ImageView with the image of the post set to match the screen width
     */
    private static ImageView generateImageContent(RedditPost post, Context context) {
        // TODO when clicked open the image so you can ZOOOOOM
        ImageView imageView = new ImageView(context);

        Picasso.get()
                .load(post.getUrl())
                .placeholder(R.drawable.ic_baseline_wifi_tethering_150)
                // Scale so the image fits the width of the screen
                .resize(MainActivity.SCREEN_WIDTH, 0)
                .into(imageView);

        return imageView;
    }

    private static PostContentLink generateLinkContent(RedditPost post, Context context) {
        PostContentLink content = new PostContentLink(context);
        content.setPost(post);

        return content;
    }

    /**
     * Generates the content for video posts
     *
     * @param post The post to generate content for
     * @return A VideoView
     */
    private static VideoView generateVideoContent(RedditPost post, Context context) {
        VideoView videoView = new VideoView(context);
        videoView.setMinimumWidth(MainActivity.SCREEN_WIDTH);
        videoView.setVideoURI(Uri.parse(post.getVideoUrl()));
        videoView.start();

        return videoView;
    }

}
