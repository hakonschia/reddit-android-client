package com.example.hakonsreader.enums;

/**
 * Enum describing how NSFW previews should be shown
 */
public enum ShowNsfwPreview {
    /**
     * Show as normal, ie. don't provide any filter on the images/video thumbnails
     */
    NORMAL,

    /**
     * Blur images/video thumbnails
     */
    BLURRED,

    /**
     * Show no image at all for the images/video thumbnails. Should still show a generic image
     * to display that something is here, but nothing that is related to the post itself
     */
    NO_IMAGE
}