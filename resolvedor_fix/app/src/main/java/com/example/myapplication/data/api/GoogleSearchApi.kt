package com.example.myapplication.data.api

import com.google.gson.annotations.SerializedName

data class GoogleSearchResponse(
    val items: List<SearchItem>? = null
)

data class SearchItem(
    val link: String
)



