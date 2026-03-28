package com.example.myapplication.data.api

import com.google.gson.annotations.SerializedName

data class YouTubeSearchResponse(
    val items: List<YouTubeItem>? = null
)

data class YouTubeItem(
    val id: VideoId
)

data class VideoId(
    val videoId: String
)



