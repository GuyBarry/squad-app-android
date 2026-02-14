package com.example.squadapp.models

import com.google.gson.annotations.SerializedName

data class RawgGamesResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<RawgGame>
)

data class RawgGame(
    val id: Int,
    val name: String,
    val slug: String,
    val released: String?,
    @SerializedName("background_image")
    val backgroundImage: String?,
    val rating: Double?,
    @SerializedName("ratings_count")
    val ratingsCount: Int?,
    @SerializedName("metacritic")
    val metacritic: Int?,
    val platforms: List<PlatformInfo>?,
    val genres: List<GenreInfo>?,
    val description: String?,
    @SerializedName("short_screenshots")
    val shortScreenshots: List<ScreenshotInfo>?
)

data class PlatformInfo(
    val platform: Platform?
)

data class Platform(
    val id: Int,
    val name: String,
    val slug: String
)

data class GenreInfo(
    val id: Int,
    val name: String,
    val slug: String
)

data class ScreenshotInfo(
    val id: Int,
    val image: String
)

