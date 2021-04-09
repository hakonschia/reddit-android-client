package com.example.hakonsreader.api.interfaces

/**
 * Interface defining images, such as preview images for Reddit posts and image in Imgur albums
 */
interface Image {

    /**
     * The URL to the image
     */
    val url: String

    /**
     * The height of the image
     */
    val height: Int

    /**
     * The width of the image
     */
    val width: Int

}