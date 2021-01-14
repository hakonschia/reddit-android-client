package com.example.hakonsreader.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import com.example.hakonsreader.databinding.ContentVideoBinding
import com.example.hakonsreader.databinding.ContentYoutubeVideoBinding
import kr.co.prnd.YouTubePlayerView

class ContentYoutubeVideo : Content {
    private val youtubePlayerView: YouTubePlayerView

    constructor(context: Context?) : super(context) {
        youtubePlayerView = ContentYoutubeVideoBinding.inflate(LayoutInflater.from(context), this, true).youtubePlayer
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        youtubePlayerView = ContentYoutubeVideoBinding.inflate(LayoutInflater.from(context), this, true).youtubePlayer
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        youtubePlayerView = ContentYoutubeVideoBinding.inflate(LayoutInflater.from(context), this, true).youtubePlayer
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        youtubePlayerView = ContentYoutubeVideoBinding.inflate(LayoutInflater.from(context), this, true).youtubePlayer
    }

    override fun updateView() {
        val youtubeUrl = redditPost.url
        val asUri = Uri.parse(youtubeUrl)
        val videoId = asUri.getQueryParameter("v") ?: return

        youtubePlayerView.play(videoId, null)
    }
}