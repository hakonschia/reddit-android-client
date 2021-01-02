package com.example.hakonsreader.api.interfaces

interface ThirdPartyGif {

    var mp4Url: String
    var mp4Size: Int

    var width: Int
    var height: Int

    var thumbnail: String

    var hasAudio: Boolean
}