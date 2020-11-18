package com.example.hakonsreader.api.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.TypeConverters;

import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.flairs.RichtextFlair;
import com.example.hakonsreader.api.persistence.PostConverter;
import com.example.hakonsreader.api.utils.MarkdownAdjuster;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @SerializedName("archived")
    private boolean isArchived;

    @SerializedName("post_hint")
    private String postHint;

    @SerializedName("domain")
    private String domain;

    @SerializedName("removed_by_category")
    private String removedByCategory;

    @SerializedName("saved")
    private boolean saved;

    @SerializedName("can_mod_post")
    private boolean isUserMod;


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

    /**
     * This holds the data for gallery items. The objects in this are either strings or other
     * {@link LinkedTreeMap}. The source image is found in a {@link LinkedTreeMap} called "s"
     */
    @SerializedName("media_metadata")
    private LinkedTreeMap<String, Object> mediaMetadata;

    private List<Image> galleryImages = null;

    /**
     * Retrieves the list of images
     * @return
     */
    public List<Image> getGalleryImages() {
        // The gallery images can be set manually when loaded from an Imgur album
        // mediaMetaData is for Reddit galleries, so in that case it will be null
        if (mediaMetadata != null) {
            galleryImages = new ArrayList<>(mediaMetadata.size());
            Gson gson = new Gson();

            mediaMetadata.forEach((s, obj) -> {
                // The source is found in the "s" object, which can be converted to a PreviewImage
                LinkedTreeMap<String, Object> converted = (LinkedTreeMap<String, Object>) obj;
                String asJson = gson.toJson(converted.get("s"));
                galleryImages.add(gson.fromJson(asJson, Image.class));
            });
        }

        return galleryImages;
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
     * Retrieve the thumbnail for the post. Note that the thumbnails are very low quality. For
     * higher quality images see {@link RedditPost#getPreviewImages()} and {@link RedditPost#getSourcePreview()}
     *
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
     * @return True if the post is archived. Archived posts cannot be voted/commented on
     */
    public boolean isArchived() {
        return isArchived;
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
     * @return Retrieves the category that removed the post (eg. "moderator"). If this is not null
     * the post has been removed
     */
    public String getRemovedByCategory() {
        return removedByCategory;
    }

    /**
     * @return True if the comment has been saved by the currently logged in user
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * @return True if the currently logged in user is a mod in the subreddit the post is in
     */
    public boolean isUserMod() {
        return isUserMod;
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
     * Retrieves the source image for the post. For image posts this will be the same image as
     * that returned by {@link RedditPost#getUrl()}. It will point to a different image, but the
     * images will be identical.
     *
     * <p>See {@link RedditPost#getPreviewImages()} for a list of different resolutions of the image</p>
     *
     * @return The source image for the post, or null if none is available
     */
    public Image getSourcePreview() {
        if (preview != null && preview.images != null) {
            return preview.images.get(0).source;
        }

        return null;
    }

    /**
     * Retrieves the list of preview images the post has. This list will usually hold a number
     * of images with different resolutions. Note that this can be empty as it's not guarnateed that
     * a post has preview images
     *
     * <p>The resolutions found here typically follow the widths below (in order), with the height matching
     * the aspect ratio:
     * <ol>
     *     <li>108</li>
     *     <li>216</li>
     *     <li>320</li>
     *     <li>640</li>
     *     <li>960</li>
     *     <li>1080</li>
     * </ol>
     * </p>
     *
     * <p>See {@link RedditPost#getSourcePreview()} for the source resolution</p>
     *
     * @return A list of preview images, or null if none is available
     * @see RedditPost#getSourcePreview()
     * @see RedditPost#getObfuscatedPreviewImages()
     */
    public List<Image> getPreviewImages() {
        if (preview != null && preview.images != null) {
            return preview.images.get(0).resolutions;
        }

        return new ArrayList<>();
    }

    /**
     * Returns a list of preview images that have been obfuscated (for NSFW posts)
     *
     * @return A list of preview images, or null if none is available
     */
    public List<Image> getObfuscatedPreviewImages() {
        try {
            List<Image> images = preview.images.get(0).variants.obfuscated.resolutions;
            return images;
        } catch (NullPointerException e) {
            return null;
        }
    }


    /**
     * Retrieves the video for GIF posts from sources such as Gfycat
     *
     * <p>Note: not all GIFs will be found here. Some GIFs will be returned as a {@link Image}
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
    public Image getMp4Source() {
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
    public List<Image> getMp4Previews() {
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

    public Preview getPreview() {
        return preview;
    }

    public LinkedTreeMap<String, Object> getMediaMetadata() {
        return mediaMetadata;
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

    public void setPreview(Preview preview) {
        this.preview = preview;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public void setMediaMetadata(LinkedTreeMap<String, Object> mediaMetadata) {
        this.mediaMetadata = mediaMetadata;
    }

    public void setRemovedByCategory(String removedByCategory) {
        this.removedByCategory = removedByCategory;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public void setUserMod(boolean userMod) {
        isUserMod = userMod;
    }

    public void setGalleryImages(List<Image> galleryImages) {
        this.galleryImages = galleryImages;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RedditPost)) return false;
        RedditPost that = (RedditPost) o;
        return amountOfComments == that.amountOfComments &&
                spoiler == that.spoiler &&
                isText == that.isText &&
                isVideo == that.isVideo &&
                isGallery == that.isGallery &&
                isArchived == that.isArchived &&
                saved == that.saved &&
                isUserMod == that.isUserMod &&
                title.equals(that.title) &&
                subreddit.equals(that.subreddit) &&
                Objects.equals(thumbnail, that.thumbnail) &&
                Objects.equals(selftext, that.selftext) &&
                Objects.equals(selftextHtml, that.selftextHtml) &&
                Objects.equals(crosspostParentID, that.crosspostParentID) &&
                Objects.equals(crossposts, that.crossposts) &&
                Objects.equals(crosspostIds, that.crosspostIds) &&
                Objects.equals(postHint, that.postHint) &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(removedByCategory, that.removedByCategory) &&
                Objects.equals(authorFlairBackgroundColor, that.authorFlairBackgroundColor) &&
                Objects.equals(authorFlairTextColor, that.authorFlairTextColor) &&
                Objects.equals(authorFlairText, that.authorFlairText) &&
                Objects.equals(authorRichtextFlairs, that.authorRichtextFlairs) &&
                Objects.equals(linkFlairBackgroundColor, that.linkFlairBackgroundColor) &&
                Objects.equals(linkFlairTextColor, that.linkFlairTextColor) &&
                Objects.equals(linkFlairText, that.linkFlairText) &&
                Objects.equals(linkRichtextFlairs, that.linkRichtextFlairs) &&
                Objects.equals(media, that.media) &&
                Objects.equals(mediaMetadata, that.mediaMetadata) &&
                Objects.equals(galleryImages, that.galleryImages) &&
                Objects.equals(preview, that.preview);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, amountOfComments, subreddit, thumbnail, spoiler, selftext, selftextHtml, crosspostParentID, crossposts, crosspostIds, isText, isVideo, isGallery, isArchived, postHint, domain, removedByCategory, saved, isUserMod, authorFlairBackgroundColor, authorFlairTextColor, authorFlairText, authorRichtextFlairs, linkFlairBackgroundColor, linkFlairTextColor, linkFlairText, linkRichtextFlairs, media, mediaMetadata, galleryImages, preview);
    }
}
