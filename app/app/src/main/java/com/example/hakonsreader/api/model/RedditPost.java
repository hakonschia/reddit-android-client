package com.example.hakonsreader.api.model;

import android.util.Log;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.TypeConverters;

import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.flairs.RichtextFlair;
import com.example.hakonsreader.api.persistence.PostConverter;
import com.example.hakonsreader.api.utils.MarkdownAdjuster;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class representing a Reddit post
 */
@Entity(tableName = "posts")
@TypeConverters({PostConverter.class})
public class RedditPost extends RedditListing {


    @SerializedName("title")
    private String title;

    @SerializedName("num_comments")
    private int amountOfComments;

    @SerializedName("subreddit")
    protected String subreddit;

    @SerializedName("thumbnail")
    private String thumbnail;

    @SerializedName("spoiler")
    private boolean spoiler;

    @SerializedName("selftext")
    private String selftext;

    @SerializedName("selftext_html")
    private String selftextHtml;

    @SerializedName("crosspost_parent")
    private String crosspostParentID;

    @SerializedName("crosspost_parent_list")
    @Ignore
    private List<RedditPost> crossposts;

    private List<String> crosspostIds;


    @SerializedName("is_self")
    private boolean isText;

    @SerializedName("is_video")
    private boolean isVideo;

    @SerializedName("is_gallery")
    private boolean isGallery;

    @SerializedName("post_hint")
    private String postHint;

    @SerializedName("domain")
    private String domain;


    @SerializedName("author_flair_background_color")
    private String authorFlairBackgroundColor;

    @SerializedName("author_flair_text_color")
    private String authorFlairTextColor;

    @SerializedName("author_flair_text")
    private String authorFlairText;

    @SerializedName("author_flair_richtext")
    @Ignore
    private List<RichtextFlair> authorRichtextFlairs;

    @SerializedName("link_flair_background_color")
    private String linkFlairBackgroundColor;

    @SerializedName("link_flair_text_color")
    private String linkFlairTextColor;

    @SerializedName("link_flair_text")
    private String linkFlairText;

    @SerializedName("link_flair_richtext")
    @Ignore
    private List<RichtextFlair> linkRichtextFlairs;


    @SerializedName("media")
    private Media media;

    /**
     * Data for video posts
     */
    public static class Media {

        @SerializedName("reddit_video")
        private RedditVideo redditVideo;
    }


    @SerializedName("gallery_data")
    private GalleryData galleryData;

    /**
     * Data for gallery posts (multiple images
     */
    public static class GalleryData {
        @SerializedName("items")
        private List<GalleryItem> items;
    }


    @SerializedName("preview")
    private Preview preview;

    public static class Preview {
        @SerializedName("images")
        private List<ImagesWrapper> images;

        /**
         * For gifs uploaded to gfycat this object holds an object pointing to the DASH url for the video
         */
        @SerializedName("reddit_video_preview")
        private RedditVideo videoPreview;
    }


    /**
     * @return The title of the post
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return The subreddit the listing is located in
     */
    public String getSubreddit() {
        return subreddit;
    }

    /**
     * @return The amount of comments the post has
     */
    public int getAmountOfComments() {
        return amountOfComments;
    }

    /**
     * @return The URL to the thumbnail of the post
     */
    public String getThumbnail() {
        return thumbnail;
    }

    /**
     * @return True if the post is marked as a spoiler
     */
    public boolean isSpoiler() {
        return spoiler;
    }

    /**
     * Retrieve the markdown text of the post.
     *
     * <p>Note: Some Reddit markdown differs from the specification, such as no space between
     * the markdown symbol and the text. See {@link MarkdownAdjuster} to fix these errors</p>
     *
     * @return The markdown text of the post if the post is {@link PostType#TEXT}
     */
    public String getSelftext() {
        return selftext;
    }

    /**
     * @return The HTML text of the post if the post is {@link PostType#TEXT}
     */
    public String getSelftextHTML() {
        return selftextHtml;
    }

    /**
     * @return The ID of the post this post is a crosspost from
     */
    public String getCrosspostParentID() {
        return crosspostParentID;
    }

    /**
     * @return The list of parent crossposts of this post
     */
    public List<RedditPost> getCrossposts() {
        return crossposts;
    }

    public List<String> getCrosspostIds() {
        return crosspostIds;
    }

    /**
     * @return The domain the post is posted to
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return The background color of the flair
     */
    public String getAuthorFlairBackgroundColor() {
        return authorFlairBackgroundColor;
    }

    /**
     * @return The text color of the author flair
     */
    public String getAuthorFlairTextColor() {
        return authorFlairTextColor;
    }

    /**
     * Retrieve the raw text of a author flair.
     *
     * <p>For rich text flairs see {@link RedditPost#getAuthorRichtextFlairs()}</p>
     *
     * @return The text of the author flair
     */
    public String getAuthorFlairText() {
        return authorFlairText;
    }

    /**
     * Retrieve the list of richtext flairs this author flair is comprised of.
     *
     * <p>For standard text flairs see {@link RedditPost#getAuthorFlairText()}</p>
     *
     * @return The list of richtext flairs for the post
     */
    public List<RichtextFlair> getAuthorRichtextFlairs() {
        return authorRichtextFlairs;
    }


    /**
     * @return The background color of the flair
     */
    public String getLinkFlairBackgroundColor() {
        return linkFlairBackgroundColor;
    }

    /**
     * @return The text color of the link flair
     */
    public String getLinkFlairTextColor() {
        return linkFlairTextColor;
    }

    /**
     * Retrieve the raw text of a link flair.
     *
     * <p>For rich text flairs see {@link RedditPost#getLinkRichtextFlairs()}</p>
     *
     * @return The text of the link flair
     */
    public String getLinkFlairText() {
        return linkFlairText;
    }

    /**
     * Retrieve the list of richtext flairs this link flair is comprised of.
     *
     * <p>For standard text flairs see {@link RedditPost#getLinkFlairText()}</p>
     *
     * @return The list of richtext flairs for the post
     */
    public List<RichtextFlair> getLinkRichtextFlairs() {
        return linkRichtextFlairs;
    }

    /**
     * Retrieve the {@link RedditVideo} object. If {@link RedditPost#getPostType()} isn't
     * {@link PostType#VIDEO}, this will be null.
     *
     * @return The video object containing information about the video post
     */
    public RedditVideo getVideo() {
        if (media == null) {
            return null;
        }

        return media.redditVideo;
    }

    /**
     * Retrieves the list of gallery items. If {@link RedditPost#getPostType()} isn't {@link PostType#GALLERY}
     * this will be null
     *
     * @return The list of gallery items this post has
     */
    public List<GalleryItem> getGalleryItems() {
        return galleryData.items;
    }

    /**
     * Retrieves the source image for the post. For image posts this will be the same image as
     * that returned by {@link RedditPost#getUrl()}. It will point to a different image, but the
     * images will be identical.
     *
     * <p>See {@link RedditPost#getPreviewImages()} for a list of different resolutions of the image</p>
     *
     * @return The source image for the post
     */
    public PreviewImage getSourcePreview() {
        return preview.images.get(0).source;
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
        return preview.images.get(0).resolutions;
    }

    /**
     * Retrieves the video for GIF posts from sources such as Gfycat
     *
     * <p>Note: not all GIFs will be found here. Some GIFs will be returned as a {@link PreviewImage}
     * with a link to the external URL. See {@link RedditPost#getMp4Source()} and {@link RedditPost#getMp4Previews()}</p>
     *
     * @return The {@link RedditVideo} holding the data for GIF posts
     */
    public RedditVideo getVideoGif() {
        return preview.videoPreview;
    }

    /**
     * Gets the MP4 source for the post. Note that this is only set when the post is a GIF
     * uploaded to reddit directly.
     *
     * <p>Note: Some GIF posts will be as a separate {@link RedditVideo} that is retrieved with
     * {@link RedditPost#getVideoGif()}</p>
     * <p>For a list of other resolutions see {@link RedditPost#getMp4Previews()}</p>
     *
     * @return The source MP4
     */
    public PreviewImage getMp4Source() {
        return preview.images.get(0).variants.mp4.source;
    }

    /**
     * Gets the list of MP4 resolutions for the post. Note that this is only set when the post is a GIF
     * uploaded to reddit directly.
     *
     * <p>For the source resolution see {@link RedditPost#getMp4Source()}</p>
     *
     * @return The list of MP4 resolutions
     */
    public List<PreviewImage> getMp4Previews() {
        return preview.images.get(0).variants.mp4.resolutions;
    }

    public String getSelftextHtml() {
        return selftextHtml;
    }

    public boolean isText() {
        return isText;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public boolean isGallery() {
        return isGallery;
    }

    public String getPostHint() {
        return postHint;
    }

    public Media getMedia() {
        return media;
    }

    public GalleryData getGalleryData() {
        return galleryData;
    }

    public Preview getPreview() {
        return preview;
    }


    /* ----------------- Setters ----------------- */
    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public void setAmountOfComments(int amountOfComments) {
        this.amountOfComments = amountOfComments;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setSpoiler(boolean spoiler) {
        this.spoiler = spoiler;
    }

    public void setSelftext(String selftext) {
        this.selftext = selftext;
    }

    public void setSelftextHtml(String selftextHtml) {
        this.selftextHtml = selftextHtml;
    }

    public void setCrosspostParentID(String crosspostParentID) {
        this.crosspostParentID = crosspostParentID;
    }

    public void setCrossposts(List<RedditPost> crossposts) {
        this.crossposts = crossposts;
    }

    public void setCrosspostIds(List<String> crosspostIds) {
        this.crosspostIds = crosspostIds;
    }

    public void setText(boolean text) {
        isText = text;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    public void setGallery(boolean gallery) {
        isGallery = gallery;
    }

    public void setPostHint(String postHint) {
        this.postHint = postHint;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setAuthorFlairBackgroundColor(String authorFlairBackgroundColor) {
        this.authorFlairBackgroundColor = authorFlairBackgroundColor;
    }

    public void setAuthorFlairTextColor(String authorFlairTextColor) {
        this.authorFlairTextColor = authorFlairTextColor;
    }

    public void setAuthorFlairText(String authorFlairText) {
        this.authorFlairText = authorFlairText;
    }

    public void setAuthorRichtextFlairs(List<RichtextFlair> authorRichtextFlairs) {
        this.authorRichtextFlairs = authorRichtextFlairs;
    }

    public void setLinkFlairBackgroundColor(String linkFlairBackgroundColor) {
        this.linkFlairBackgroundColor = linkFlairBackgroundColor;
    }

    public void setLinkFlairTextColor(String linkFlairTextColor) {
        this.linkFlairTextColor = linkFlairTextColor;
    }

    public void setLinkFlairText(String linkFlairText) {
        this.linkFlairText = linkFlairText;
    }

    public void setLinkRichtextFlairs(List<RichtextFlair> linkRichtextFlairs) {
        this.linkRichtextFlairs = linkRichtextFlairs;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public void setGalleryData(GalleryData galleryData) {
        this.galleryData = galleryData;
    }

    public void setPreview(Preview preview) {
        this.preview = preview;
    }

    /**
     * @return The type of post (image, video, text, or link)
     */
    public PostType getPostType() {
        // TODO make this less bad
        if (isVideo) {
            return PostType.VIDEO;
        } else if (isText) {
            return PostType.TEXT;
        } else if (isGallery) {
            return PostType.GALLERY;
        } else if (crosspostParentID != null) {
            return PostType.CROSSPOST;
        }

        // Usually no hint means it's a text post, but sometimes it means it's a link post
        // If the url for the post isn't to reddit, it's a link post (these link posts don't have a thumbnail for some reason)
        if (postHint == null) {
            if (url.matches("https://www\\.reddit\\.com")) {
                return PostType.TEXT;
            } else {
                return PostType.LINK;
            }
        }

        if (postHint.equals("link")) {
            // If link matches "imgur.com/...", add a .png to the end and it will redirect to the direct link

            // If we have a link post that is a link to imgur, redirect it to get the image directly
            // The IDs are (as far as I know) alphanumerical (upper and lowercase) and 0-9 with 5 or 7 digits
            // (it's currently between 5 and 7 characters but it works good enough I guess)
            if (url.matches("^https://imgur.com/[A-Za-z0-9]{5,7}$")) {
                // Technically the URL needs to be "i.imgur.com/...", but imgur does the redirection for us
                // Which suffix is added (.png or .jpg or .jpeg) seemingly doesn't matter
                url += ".png";
            }

            // Link posts might be images not uploaded to reddit
            // Technically this doesn't match just URLs as anything can preceed the .png/.jpg/.jpeg
            // but that shouldn't really matter
            if (url.matches(".+(.png|.jpeg|.jpg)$")) {
                return PostType.IMAGE;
            } else if (url.endsWith(".gifv")) {
                return PostType.GIF;
            }

            return PostType.LINK;
        }

        switch (postHint) {
            case "image":
                // .gif is treated as image
                if (url.endsWith(".gif")) {
                    return PostType.GIF;
                }

                return PostType.IMAGE;

            case "hosted:video":
                return PostType.VIDEO;

            case "rich:video":
                return PostType.RICH_VIDEO;

            // This should never happen, so if it does it's better to get a NPE to find cause an exception
            default:
                return null;
        }
    }
}
