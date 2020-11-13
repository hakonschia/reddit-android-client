package com.example.hakonsreader.api.enums;

/**
 * Enum representing what a post is
 */
public enum PostType {
    /**
     * Text posts (self posts)
     */
    TEXT,

    /**
     * Link posts, post is a link to somewhere else
     */
    LINK,

    /**
     * Image post
     */
    IMAGE,

    /**
     * Gallery posts (multiple images shown as a gallery)
     */
    GALLERY,

    /**
     * Video post that is hosted on Reddit
     */
    VIDEO,

    /**
     * Video post that is not hosted on reddit. For instance, YouTube links will be classified as
     * RICH_VIDEO
     */
    RICH_VIDEO,

    /**
     * Gif post. This can be hosted on Reddit or externally
     */
    GIF,

    /**
     * The post is a crosspost of another post on Reddit
     */
    CROSSPOST;
}
