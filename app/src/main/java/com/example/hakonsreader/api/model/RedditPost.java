package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.PostableListing;
import com.example.hakonsreader.api.interfaces.VotableListing;
import com.example.hakonsreader.api.jsonadapters.BooleanPrimitiveAdapter;
import com.example.hakonsreader.api.model.flairs.RichtextFlair;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class representing a Reddit post
 */
public class RedditPost implements VotableListing, PostableListing {
    private static final String TAG = "RedditPost";

    @SerializedName("kind")
    private String kind;

    @SerializedName("data")
    public Data data;


    /**
     * Post specific data
     */
    private static class Data {
        /* ------------- RedditListing ------------- */
        @SerializedName("id")
        private String id;

        @SerializedName("url")
        private String url;

        @SerializedName("name")
        private String fullname;

        @SerializedName("created_utc")
        private float createdAt;

        @SerializedName("over_18")
        private boolean nsfw;
        /* ------------- End RedditListing ------------- */

        /* ------------- PostableListing ------------- */
        @SerializedName("subreddit")
        private String subreddit;

        @SerializedName("author")
        private String author;

        @SerializedName("permalink")
        private String permalink;

        @SerializedName("is_locked")
        private boolean isLocked;

        @SerializedName("is_stickied")
        private boolean isStickied;

        /**
         * What the listing is distinguished as (such as "moderator")
         */
        @SerializedName("distinguished")
        private String distinguished;
        /* ------------- End PostableListing ------------- */

        /* ------------- End VoteableListing ------------- */
        @SerializedName("score")
        private int score;

        @SerializedName("score_hidden")
        private boolean scoreHidden;

        @SerializedName("likes")
        @JsonAdapter(BooleanPrimitiveAdapter.class)
        private Boolean liked;
        /* ------------- End VoteableListing ------------- */



        @SerializedName("title")
        private String title;

        @SerializedName("num_comments")
        private int amountOfComments;

        @SerializedName("thumbnail")
        private String thumbnail;

        @SerializedName("spoiler")
        private boolean spoiler;

        @SerializedName("selftext_html")
        private String selftextHtml;

        @SerializedName("crosspost_parent")
        private String crosspostParentID;

        @SerializedName("crosspost_parent_list")
        private List<RedditPost> crossposts;


        @SerializedName("is_self")
        private boolean isText;

        @SerializedName("is_video")
        private boolean isVideo;

        @SerializedName("is_gallery")
        private boolean isGallery;

        @SerializedName("post_hint")
        private String postHint;


        @SerializedName("link_flair_background_color")
        private String linkFlairBackgroundColor;

        @SerializedName("link_flair_text_color")
        private String linkFlairTextColor;

        @SerializedName("link_flair_richtext")
        private List<RichtextFlair> linkRichtextFlairs;


        @SerializedName("media")
        private Media media;

        /**
         * Data for video posts
         */
        private static class Media {

            @SerializedName("reddit_video")
            private RedditVideo redditVideo;
        }


        @SerializedName("gallery_data")
        private GalleryData galleryData;

        /**
         * Data for gallery posts (multiple images
         */
        private static class GalleryData {
            @SerializedName("items")
            private List<GalleryItem> items;
        }


        @SerializedName("preview")
        private Preview preview;

        private static class Preview {
            @SerializedName("images")
            ImagesWrapper images;
        }
    }


    /* --------------------- Inherited --------------------- */
    /* ------------- RedditListing ------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    public String getKind() {
        return kind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getID() {
        return data.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getURL() {
        return data.url;
    }

    /**
     * {@inheritDoc}
     */@Override
    public String getFullname() {
        return data.fullname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCreatedAt() {
        return (long)data.createdAt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNSFW() {
        return data.nsfw;
    }
    /* ------------- End RedditListing ------------- */

    /* ------------- PostableListing ------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    public String getSubreddit() {
        return data.subreddit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthor() {
        return data.author;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPermalink() {
        return data.permalink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocked() {
        return data.isLocked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStickied() {
        return data.isStickied;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMod() {
        if (data.distinguished == null) {
            return false;
        }

        return data.distinguished.equals("moderator");
    }
    /* ------------- End PostableListing ------------- */

    /* ------------- VoteableListing ------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    public int getScore() {
        return data.score;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isScoreHidden() {
        return data.scoreHidden;
    }

    /**
     * Retrieves the logged in users vote on the post
     *
     * @return If upvoted, VoteType.Upvote. If downvoted VoteType.Downvote
     */
    public VoteType getVoteType() {
        if (data.liked == null) {
            return VoteType.NO_VOTE;
        }

        return (data.liked ? VoteType.UPVOTE : VoteType.DOWNVOTE);
    }

    /**
     * @param voteType The vote type for this post for the current user
     */
    public void setVoteType(VoteType voteType) {
        // Update the internal data as that is used in getVoteType
        switch (voteType) {
            case UPVOTE:
                data.liked = true;
                break;
            case DOWNVOTE:
                data.liked = false;
                break;
            case NO_VOTE:
                data.liked = null;
                break;
        }
    }
    /* ------------- End VoteableListing ------------- */
    /* --------------------- End inherited --------------------- */


    /**
     * @return The title of the post
     */
    public String getTitle() {
        return data.title;
    }

    /**
     * @return The amount of comments the post has
     */
    public int getAmountOfComments() {
        return data.amountOfComments;
    }

    /**
     * @return The URL to the thumbnail of the post
     */
    public String getThumbnail() {
        return data.thumbnail;
    }

    /**
     * @return True if the post is marked as a spoiler
     */
    public boolean isSpoiler() {
        return data.spoiler;
    }

    /**
     * @return The HTML of the text of the post if the post is {@link PostType#TEXT}
     */
    public String getSelftextHTML() {
        return data.selftextHtml;
    }

    /**
     * @return The ID of the post this post is a crosspost from
     */
    public String getCrosspostParentID() {
        return data.crosspostParentID;
    }

    /**
     * @return The list of parent crossposts of this post
     */
    public List<RedditPost> getCrossposts() {
        return data.crossposts;
    }


    public String getLinkFlairBackgroundColor() {
        return data.linkFlairBackgroundColor;
    }

    public String getLinkFlairTextColor() {
        return data.linkFlairTextColor;
    }

    /**
     * @return The list of richtext flairs for the post
     */
    public List<RichtextFlair> getLinkRichtextFlairs() {
        return data.linkRichtextFlairs;
    }

    /**
     * Retrieve the {@link RedditVideo} object. If {@link RedditPost#getPostType()} isn't
     * {@link PostType#VIDEO}, this will be null
     *
     * @return The video object containing information about the video post
     */
    public RedditVideo getRedditVideo() {
        return data.media.redditVideo;
    }

    /**
     * Retrieves the list of gallery items. If {@link RedditPost#getPostType()} isn't {@link PostType#GALLERY}
     * this will be null
     *
     * @return The list of gallery items this post has
     */
    public List<GalleryItem> getGalleryItems() {
         return data.galleryData.items;
    }

    /**
     * Retrieves the source image for the post. For image posts this will be the same image as
     * that returned by {@link RedditPost#getURL()}. It will point to a different image, but the
     * images will be identical.
     *
     * <p>See {@link RedditPost#getPreviewImages()} for a list of different resolutions of the image</p>
     *
     * @return The source image for the post
     */
    public PreviewImage getSourcePreview() {
        return data.preview.images.source;
    }

    /**
     * Retrieves the list of preview images the post has. This list will usually hold a number
     * of images with different resolutions.
     *
     * <p>See {@link RedditPost#getSourcePreview()} for the source resolution</p>
     *
     * @return A list of preview images
     */
    public List<PreviewImage> getPreviewImages() {
        return data.preview.images.resolutions;
    }


    /**
     * @return The type of post (image, video, text, or link)
     */
    public PostType getPostType() {
        // TODO make this less bad
        if (data.isVideo) {
            return PostType.VIDEO;
        } else if (data.isText) {
            return PostType.TEXT;
        } else if (data.isGallery) {
            return PostType.GALLERY;
        } else if (data.crosspostParentID != null) {
            return PostType.CROSSPOST;
        }

        String hint = data.postHint;

        // Text posts don't have a hint
        if (hint == null) {
            return PostType.TEXT;
        }

        if (hint.equals("link")) {
            // If link matches "imgur.com/...", add a .png to the end and it will redirect to the direct link

            // If we have a link post that is a link to imgur, redirect it to get the image directly
            // The IDs are (as far as I know) alphanumerical (upper and lowercase) and 0-9 with 5 or 7 digits
            // (it's currently between 5 and 7 characters but it works good enough I guess)
            if (data.url.matches("^https://imgur.com/[A-Za-z0-9]{5,7}$")) {
                // Technically the URL needs to be "i.imgur.com/...", but imgur does the redirection for us
                // Which suffix is added (.png or .jpg or .jpeg) seemingly doesn't matter
                data.url += ".png";
            }

            // Link posts might be images not uploaded to reddit
            // Technically this doesn't match just URLs as anything can preceed the .png/.jpg/.jpeg
            // but that shouldn't really matter
            if (data.url.matches(".+(.png|.jpeg|.jpg)$")) {
                return PostType.IMAGE;
            } else if (data.url.endsWith(".gifv")) {
                return PostType.GIF;
            }

            return PostType.LINK;
        }

        switch (hint) {
            case "image":
                // .gif is treated as image
                if (data.url.endsWith(".gif")) {
                    return PostType.GIF;
                }

                return PostType.IMAGE;

            case "hosted:video":
                return PostType.VIDEO;

            case "rich:video":
                return PostType.RICH_VIDEO;

            // No hint means it's a text post
            default:
                return PostType.TEXT;
        }
    }
}
