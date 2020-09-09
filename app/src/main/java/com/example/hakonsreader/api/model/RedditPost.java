package com.example.hakonsreader.api.model;

/**
 * Class representing a Reddit post
 */
public class RedditPost extends RedditListing {
    private static final String TAG = "RedditPost";

    public enum PostType {
        Image, Video, RichVideo, Link, Text
    }

    /**
     * Create a post object from a base listing
     *
     * @param base The base listing to create from
     * @return A post object with the values from {@code base}
     */
    public static RedditPost createFromListing(RedditListing base) {
        return base.createFromListing(RedditPost.class);
    }

    /**
     * @return The type of post (image, video, text, or link)
     */
    public PostType getPostType() {
        // TODO make this less bad
        // TODO reddit galleries (multiple images)
        if (data.isVideo) {
            return PostType.Video;
        } else if (data.isText) {
            return PostType.Text;
        }

        String hint = data.postHint;

        // Text posts don't have a hint
        if (hint == null) {
            return PostType.Text;
        }

        if (hint.equals("link")) {
            // Link posts might be images not uploaded to reddit
            if (hint.matches("(.png|.jpeg|.jpg)$")) {
                return PostType.Image;
            } else if (hint.matches(".gifv")) {
                return PostType.Video;
            }

            return PostType.Link;
        }

        switch (hint) {
            case "image":
                // .gif is treated as image
                if (data.url.endsWith(".gif")) {
                    return PostType.Video;
                }

                return PostType.Image;

            case "hosted:video":
                return PostType.Video;

            case "rich:video":
                return PostType.RichVideo;

            // No hint means it's a text post
            default:
                return PostType.Text;
        }
    }

}
