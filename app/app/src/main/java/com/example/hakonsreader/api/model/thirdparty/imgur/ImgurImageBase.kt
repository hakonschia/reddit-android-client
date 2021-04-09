package com.example.hakonsreader.api.model.thirdparty.imgur

/**
 * Base image from Imgur, identical to [ImgurImage], but is not abstract
 */
// The purpose of this is to be able to deserialize a base Imgur image and ImgurGif automatically
// If we only have this (which would then be identical to how ImgurImage is now, but not abstract)
// we couldn't use ImgurImageAdapter as it would cause an infinite loop when it reached an ImgurImage
class ImgurImageBase : ImgurImage()